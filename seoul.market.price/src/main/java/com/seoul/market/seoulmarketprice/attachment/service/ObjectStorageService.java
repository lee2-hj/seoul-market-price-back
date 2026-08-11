package com.seoul.market.seoulmarketprice.attachment.service;

import org.springframework.web.multipart.MultipartFile;

/** 첨부파일 서비스가 특정 객체 스토리지 SDK에 의존하지 않도록 저장 연산을 추상화한다. */
public interface ObjectStorageService {
    /** 전달받은 파일을 지정한 객체 키로 저장한다. */
    void upload(String objectKey, MultipartFile file);
    /** 지정한 객체 키의 파일을 물리적으로 삭제한다. */
    void delete(String objectKey);
    /** 제한 시간 동안 유효한 비공개 객체 다운로드 URL을 생성한다. */
    String createDownloadUrl(String objectKey, String originalName, int expirySeconds);
}
