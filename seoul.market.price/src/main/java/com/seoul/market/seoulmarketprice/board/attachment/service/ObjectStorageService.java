package com.seoul.market.seoulmarketprice.board.attachment.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일 서비스가 MinIO SDK 구현에 직접 의존하지 않도록 객체 저장 연산을 추상화한다.
 * 구현체가 바뀌더라도 업무 서비스는 객체 키를 기준으로 업로드·삭제·다운로드 URL 발급만 요청한다.
 */
public interface ObjectStorageService {
    /**
     * 전달받은 파일 스트림을 지정한 객체 키에 저장한다.
     *
     * @param objectKey 버킷 내부에서 객체를 식별하는 전체 키
     * @param file 저장할 multipart 파일
     * @throws IllegalStateException 객체 스토리지 저장에 실패한 경우
     */
    void upload(String objectKey, MultipartFile file);

    /**
     * 지정한 객체 키의 파일을 객체 스토리지에서 물리적으로 삭제한다.
     *
     * @param objectKey 삭제할 객체의 전체 키
     * @throws IllegalStateException 객체 스토리지 삭제에 실패한 경우
     */
    void delete(String objectKey);

    /**
     * 비공개 객체를 제한 시간 동안 내려받을 수 있는 서명 URL을 생성한다.
     *
     * @param objectKey 다운로드할 객체의 전체 키
     * @param originalName 다운로드 응답에 사용할 사용자 원본 파일명
     * @param expirySeconds URL 유효시간(초)
     * @return 인증 정보가 서명된 임시 다운로드 URL
     * @throws IllegalStateException URL 생성에 실패한 경우
     */
    String createDownloadUrl(String objectKey, String originalName, int expirySeconds);
}
