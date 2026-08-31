package com.seoul.market.seoulmarketprice.ai.repository;

import com.seoul.market.seoulmarketprice.ai.config.ApartmentDatasetProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@ConditionalOnProperty(prefix = "app.datasets.apartment-main", name = "mode", havingValue = "minio")
public class MinioParquetApartmentLocationRepository implements ApartmentLocationRepository {
    private static final Logger log = LoggerFactory.getLogger(MinioParquetApartmentLocationRepository.class);

    private final MinioClient minioClient;
    private final ApartmentDatasetProperties properties;
    private volatile Cache cache;

    public MinioParquetApartmentLocationRepository(MinioClient minioClient,
                                                    ApartmentDatasetProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        try {
            locations();
            return true;
        } catch (RuntimeException exception) {
            log.warn("아파트 Parquet 데이터셋을 읽을 수 없습니다: {}", properties.location(), exception);
            return false;
        }
    }

    @Override
    public String datasetLocation() {
        Cache current = cache;
        return current == null ? properties.location() : properties.location() + current.partition() + "/";
    }

    @Override
    public List<ApartmentLocation> findCandidates(double latitude, double longitude, int radiusMeters) {
        double latitudeDelta = radiusMeters / 111_320.0;
        double longitudeScale = Math.max(0.01, Math.cos(Math.toRadians(latitude)));
        double longitudeDelta = radiusMeters / (111_320.0 * longitudeScale);
        return locations().stream()
                .filter(item -> item.latitude() != null && item.longitude() != null)
                .filter(item -> Math.abs(item.latitude() - latitude) <= latitudeDelta)
                .filter(item -> Math.abs(item.longitude() - longitude) <= longitudeDelta)
                .toList();
    }

    @Override
    public List<ApartmentLocation> findByRegion(String sggCode, String dongCode) {
        return locations().stream()
                .filter(item -> sggCode.equals(item.sggCode()))
                .filter(item -> dongCode == null || dongCode.equals(item.dongCode()))
                .toList();
    }

    private List<ApartmentLocation> locations() {
        Cache current = cache;
        long now = System.currentTimeMillis();
        if (current != null && now - current.loadedAtMillis()
                < properties.effectiveCacheTtlSeconds() * 1000) {
            return current.locations();
        }
        synchronized (this) {
            current = cache;
            if (current != null && now - current.loadedAtMillis()
                    < properties.effectiveCacheTtlSeconds() * 1000) {
                return current.locations();
            }
            cache = loadLatestPartition(now);
            return cache.locations();
        }
    }

    private Cache loadLatestPartition(long loadedAtMillis) {
        try {
            List<String> objects = listParquetObjects();
            String partition = objects.stream()
                    .map(this::partitionOf)
                    .filter(Objects::nonNull)
                    .max(String::compareTo)
                    .orElseThrow(() -> new IllegalStateException("Parquet 파티션을 찾을 수 없습니다."));
            List<String> latestObjects = objects.stream()
                    .filter(name -> partition.equals(partitionOf(name)))
                    .toList();
            List<ApartmentLocation> loaded = new ArrayList<>();
            for (String objectName : latestObjects) {
                loaded.addAll(readParquet(objectName));
            }
            Map<String, ApartmentAccumulator> unique = new LinkedHashMap<>();
            for (ApartmentLocation location : loaded) {
                unique.computeIfAbsent(location.apartmentId() + "-" + areaKey(location.exclusiveAreaM2()),
                                ignored -> new ApartmentAccumulator(location))
                        .add(location);
            }
            List<ApartmentLocation> locations = unique.values().stream()
                    .map(ApartmentAccumulator::toLocation)
                    .toList();
            log.info("아파트 위치 데이터셋 적재 완료: partition={}, files={}, rows={}",
                    partition, latestObjects.size(), locations.size());
            return new Cache(partition, locations, loadedAtMillis);
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO Parquet 데이터셋 조회에 실패했습니다.", exception);
        }
    }

    private List<String> listParquetObjects() throws Exception {
        List<String> names = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(properties.bucket())
                .prefix(normalizedPrefix())
                .recursive(true)
                .build());
        for (Result<Item> result : results) {
            String name = result.get().objectName();
            if (name.toLowerCase().endsWith(".parquet")) names.add(name);
        }
        return names;
    }

    private List<ApartmentLocation> readParquet(String objectName) throws Exception {
        byte[] bytes;
        try (GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectName)
                .build()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            response.transferTo(output);
            bytes = output.toByteArray();
        }
        List<ApartmentLocation> result = new ArrayList<>();
        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(new ByteArrayInputFile(bytes)).build()) {
            GenericRecord row;
            while ((row = reader.read()) != null) {
                Double latitude = number(row, "latitude");
                Double longitude = number(row, "longitude");
                String cggCode = text(row, "cgg_cd");
                String dongCode = text(row, "stdg_cd");
                String mainNumber = text(row, "mno");
                String subNumber = text(row, "sno");
                String apartmentName = text(row, "bldg_nm");
                if (latitude == null || longitude == null || apartmentName == null) continue;
                String apartmentId = String.join("-", nullToEmpty(cggCode), nullToEmpty(dongCode),
                        nullToEmpty(mainNumber), nullToEmpty(subNumber));
                String address = buildAddress(row, mainNumber, subNumber);
                result.add(new ApartmentLocation(apartmentId, apartmentName, address,
                        cggCode, dongCode, latitude, longitude,
                        longNumber(row, "total_thing_amt"), integerNumber(row, "deal_cnt"),
                        longNumber(row, "total_pyeong_amt"), numberAny(row, "exclusive_area_m2", "exclusive_area", "exclu_use_ar"), text(row, "deal_date"),
                        text(row, "base_date")));
            }
        }
        return result;
    }

    private String buildAddress(GenericRecord row, String mainNumber, String subNumber) {
        List<String> parts = new ArrayList<>();
        addText(parts, text(row, "cgg_nm"));
        addText(parts, text(row, "stdg_nm"));
        if (mainNumber != null) {
            String lot = mainNumber;
            if (subNumber != null && !subNumber.equals("0000")) lot += "-" + subNumber;
            parts.add(lot);
        }
        return String.join(" ", parts);
    }

    private void addText(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value);
    }

    private String partitionOf(String objectName) {
        String prefix = normalizedPrefix();
        if (!objectName.startsWith(prefix)) return null;
        int end = objectName.indexOf('/', prefix.length());
        if (end < 0) return null;
        String partition = objectName.substring(prefix.length(), end);
        return partition.startsWith("base_date=") ? partition : null;
    }

    private String normalizedPrefix() {
        String prefix = properties.prefix();
        while (prefix.startsWith("/")) prefix = prefix.substring(1);
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private String text(GenericRecord row, String field) {
        Object value = row.get(field);
        return value == null ? null : value.toString();
    }

    private Double number(GenericRecord row, String field) {
        Object value = row.get(field);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Double numberAny(GenericRecord row, String... fields) {
        for (String field : fields) {
            if (row.getSchema().getField(field) == null) continue;
            Double value = number(row, field);
            if (value != null) return value;
        }
        return null;
    }

    private Long longNumber(GenericRecord row, String field) {
        Object value = row.get(field);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer integerNumber(GenericRecord row, String field) {
        Object value = row.get(field);
        return value instanceof Number number ? number.intValue() : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String areaKey(Double area) {
        return area == null ? "unknown" : String.format(java.util.Locale.ROOT, "%.2f", area);
    }

    private record Cache(String partition, List<ApartmentLocation> locations, long loadedAtMillis) {}

    private static final class ApartmentAccumulator {
        private final ApartmentLocation representative;
        private long totalTradeAmount;
        private long weightedPyeongAmount;
        private int dealCount;
        private String latestDealDate;
        private String baseDate;

        private ApartmentAccumulator(ApartmentLocation representative) {
            this.representative = representative;
        }

        private void add(ApartmentLocation item) {
            int count = item.dealCount() == null ? 0 : item.dealCount();
            if (item.totalTradeAmount() != null && count > 0) {
                totalTradeAmount += item.totalTradeAmount();
                dealCount += count;
            }
            if (item.averagePyeongAmount() != null && count > 0) {
                weightedPyeongAmount += item.averagePyeongAmount() * count;
            }
            if (item.latestDealDate() != null
                    && (latestDealDate == null || item.latestDealDate().compareTo(latestDealDate) > 0)) {
                latestDealDate = item.latestDealDate();
            }
            if (item.baseDate() != null && (baseDate == null || item.baseDate().compareTo(baseDate) > 0)) {
                baseDate = item.baseDate();
            }
        }

        private ApartmentLocation toLocation() {
            Long pyeong = dealCount < 1 ? null : Math.round((double) weightedPyeongAmount / dealCount);
            return new ApartmentLocation(representative.apartmentId(), representative.apartmentName(),
                    representative.address(), representative.sggCode(), representative.dongCode(),
                    representative.latitude(), representative.longitude(),
                    dealCount < 1 ? null : totalTradeAmount, dealCount < 1 ? null : dealCount,
                    pyeong, representative.exclusiveAreaM2(), latestDealDate, baseDate);
        }
    }

    private static final class ByteArrayInputFile implements InputFile {
        private final byte[] data;

        private ByteArrayInputFile(byte[] data) {
            this.data = data;
        }

        @Override public long getLength() { return data.length; }
        @Override public SeekableInputStream newStream() { return new ByteArraySeekableInputStream(data); }
    }

    private static final class ByteArraySeekableInputStream extends SeekableInputStream {
        private final byte[] data;
        private int position;

        private ByteArraySeekableInputStream(byte[] data) { this.data = data; }
        @Override public long getPos() { return position; }
        @Override public void seek(long newPosition) throws IOException {
            if (newPosition < 0 || newPosition > data.length) throw new IOException("잘못된 Parquet 위치입니다.");
            position = (int) newPosition;
        }
        @Override public int read() { return position >= data.length ? -1 : data[position++] & 0xff; }
        @Override public int read(byte[] buffer, int offset, int length) {
            if (position >= data.length) return -1;
            int count = Math.min(length, data.length - position);
            System.arraycopy(data, position, buffer, offset, count);
            position += count;
            return count;
        }
        @Override public void readFully(byte[] buffer) throws IOException { readFully(buffer, 0, buffer.length); }
        @Override public void readFully(byte[] buffer, int offset, int length) throws IOException {
            if (position + length > data.length) throw new IOException("Parquet 데이터 끝에 도달했습니다.");
            System.arraycopy(data, position, buffer, offset, length);
            position += length;
        }
        @Override public int read(ByteBuffer buffer) {
            if (position >= data.length) return -1;
            int count = Math.min(buffer.remaining(), data.length - position);
            buffer.put(data, position, count);
            position += count;
            return count;
        }
        @Override public void readFully(ByteBuffer buffer) throws IOException {
            int count = buffer.remaining();
            if (position + count > data.length) throw new IOException("Parquet 데이터 끝에 도달했습니다.");
            buffer.put(data, position, count);
            position += count;
        }
    }
}
