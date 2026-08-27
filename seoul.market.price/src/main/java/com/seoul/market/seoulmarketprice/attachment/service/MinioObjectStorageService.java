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

/**
 * {@link ObjectStorageService}를 MinIO Java SDK로 구현한다.
 * 모든 연산은 설정된 단일 버킷을 사용하며 SDK 예외는 서비스 계층에서 처리할 수 있도록
 * {@link IllegalStateException}으로 변환한다.
 */
@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {
    /** S3 호환 API를 호출하는 MinIO 클라이언트. */
    private final MinioClient minioClient;
    /** 버킷 이름을 포함한 MinIO 연결 설정. */
    private final MinioProperties properties;

    /**
     * multipart 파일을 메모리에 전부 적재하지 않고 입력 스트림으로 MinIO에 업로드한다.
     *
     * @param objectKey 버킷 내부에 저장할 객체 키
     * @param file 업로드할 multipart 파일
     */
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

    /**
     * 설정된 버킷에서 객체 키에 해당하는 파일을 물리적으로 제거한다.
     *
     * @param objectKey 삭제할 객체 키
     */
    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception exception) {
            throw new IllegalStateException("첨부파일 삭제에 실패했습니다.", exception);
        }
    }

    /**
     * 원본 파일명을 RFC 5987 형식의 {@code Content-Disposition} 응답 값에 포함한 URL을 생성한다.
     * 한글과 공백이 포함된 파일명도 유지되도록 UTF-8 URL 인코딩 후 공백을 {@code %20}으로 바꾼다.
     *
     * @param objectKey 다운로드할 객체 키
     * @param originalName 브라우저에 표시할 원본 파일명
     * @param expirySeconds 서명 URL의 유효시간(초)
     * @return MinIO가 서명한 HTTP GET 다운로드 URL
     */
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
