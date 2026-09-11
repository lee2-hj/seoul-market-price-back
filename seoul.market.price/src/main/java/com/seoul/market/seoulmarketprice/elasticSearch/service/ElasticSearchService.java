package com.seoul.market.seoulmarketprice.elasticSearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocation;
import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.request.AptNameRequest;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ElasticSearchService {

    private static final String INDEX = "apt_name";
    // Elasticsearch는 size에 진짜 "무제한"을 줄 수 없고, 인덱스 기본 설정
    // index.max_result_window(기본값 10000)가 사실상의 상한이다. 이 인덱스는
    // 그 설정을 따로 늘리지 않았으므로, 그 값을 그대로 써서 실질적으로 제한이 없도록 한다.
    private static final int SEARCH_SIZE = 10000;
    private static final List<String> SOURCE_FIELDS = List.of("apt_name", "mno", "sno", "dong_cd", "dong_nm", "sgg_cd", "sgg_nm");

    private final ElasticsearchClient elasticsearchClient;
    // apt_name 인덱스는 최신 mart(Parquet) 파티션 대비 갱신이 지연될 수 있어 mno/sno가 실제 값과
    // 어긋날 수 있다(지번 정정/재계산 등). reconcileWithLatestPartition()에서 이 저장소(dm_main
    // 최신 파티션)의 값으로 mno/sno를 교정한다.
    private final ApartmentLocationRepository apartmentLocationRepository;

    // apt_name 자동완성 검색. apt_name이 비어있으면 이름 조건 없이 sgg_cd/dong_cd만으로 목록을
    // 불러온다. sgg_cd/dong_cd가 있으면(apt_name 입력 여부와 무관하게) 정확히 일치하는
    // 문서로만 필터링한다.
    public List<AptNameResponse> searchAptName(AptNameRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (StringUtils.hasText(request.apt_name())) {
            boolQuery.must(m -> m.matchPhrasePrefix(p -> p.field("apt_name").query(request.apt_name())));
        }

        if (StringUtils.hasText(request.sgg_cd())) {
            boolQuery.filter(f -> f.term(t -> t.field("sgg_cd").value(request.sgg_cd())));
        }
        if (StringUtils.hasText(request.dong_cd())) {
            boolQuery.filter(f -> f.term(t -> t.field("dong_cd").value(request.dong_cd())));
        }

        try {
            SearchResponse<AptNameResponse> response = elasticsearchClient.search(s -> s
                            .index(INDEX)
                            .size(SEARCH_SIZE)
                            .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS)))
                            .query(q -> q.bool(boolQuery.build())),
                    AptNameResponse.class);

            List<AptNameResponse> hits = response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
            return reconcileWithLatestPartition(hits);
        } catch (IOException e) {
            throw new UncheckedIOException("엘라스틱서치 apt_name 검색에 실패했습니다.", e);
        }
    }

    /**
     * ES 히트의 mno/sno를 dm_main 최신 파티션(Parquet) 기준으로 교정한다. 같은 자치구(sgg_cd)+
     * 법정동(dong_cd) 안에서 단지명이 정확히 하나로 매칭되면 그 mno/sno로 교체하고, 매칭이 없거나
     * (좌표 데이터셋 미사용 등) 동명 단지가 여러 곳이라 특정할 수 없으면 ES 값을 그대로 둔다(과도한
     * 자동 교정으로 다른 단지 값을 잘못 덮어쓰는 것을 방지).
     */
    List<AptNameResponse> reconcileWithLatestPartition(List<AptNameResponse> hits) {
        if (!apartmentLocationRepository.isAvailable()) return hits;
        return hits.stream().map(this::reconcile).toList();
    }

    private AptNameResponse reconcile(AptNameResponse hit) {
        if (!StringUtils.hasText(hit.sgg_cd()) || !StringUtils.hasText(hit.apt_name())) return hit;
        String targetName = normalized(hit.apt_name());
        List<ApartmentLocation> matches = apartmentLocationRepository
                .findByRegion(hit.sgg_cd(), hit.dong_cd()).stream()
                .filter(location -> normalized(location.apartmentName()).equals(targetName))
                .toList();
        if (matches.size() != 1) return hit;

        ApartmentLocation match = matches.get(0);
        String mainNumber = match.mainNumber();
        String subNumber = match.subNumber();
        if (mainNumber == null || subNumber == null) return hit;
        if (mainNumber.equals(hit.mno()) && subNumber.equals(hit.sno())) return hit;

        return new AptNameResponse(hit.apt_name(), mainNumber, subNumber,
                hit.dong_cd(), hit.dong_nm(), hit.sgg_cd(), hit.sgg_nm());
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("(?i)아파트|단지", "")
                .replaceAll("제\\s*(\\d+)\\s*차", "$1차")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toLowerCase(Locale.ROOT);
    }
}
