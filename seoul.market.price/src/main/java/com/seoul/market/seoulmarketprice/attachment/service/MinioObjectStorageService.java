package com.seoul.market.seoulmarketprice.attachment.service;

import com.seoul.market.seoulmarketprice.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** MinIO Java SDK를 이용해 실제 첨부파일 객체를 저장·삭제·다운로드한다. */
@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {
    /** S3 호환 API를 호출하는 MinIO 클라이언트. */
    private final MinioClient minioClient;
    /** 버킷 이름을 포함한 MinIO 연결 설정. */
    private final MinioProperties properties;

    /** Multipart 파일 스트림을 설정 버킷의 객체 키에 업로드한다. */
    @Override
    public void upload(String objectKey, MultipartFile file) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("첨부파일 저장에 실패했습니다.", exception);
        }
    }

    /** 설정 버킷에서 객체 키에 해당하는 파일을 제거한다. */
    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception exception) {
            throw new IllegalStateException("첨부파일 삭제에 실패했습니다.", exception);
        }
    }

    /** 원본 파일명을 Content-Disposition에 포함한 단기 다운로드 URL을 생성한다. */
    @Override
    public String createDownloadUrl(String objectKey, String originalName, int expirySeconds) {
        try {
            String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .extraQueryParams(Map.of(
                            "response-content-disposition",
                            "attachment; filename*=UTF-8''" + encoded
                    ))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("첨부파일 다운로드 주소 생성에 실패했습니다.", exception);
        }
    }
}
