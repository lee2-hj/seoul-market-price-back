package com.seoul.market.seoulmarketprice.board.attachment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * MinIO에 저장된 파일의 객체 키와 화면 표시용 메타데이터를 MySQL에 보관하는 엔티티다.
 * 실제 파일 데이터나 외부에서 바로 접근 가능한 URL은 DB에 저장하지 않으며, 삭제 시 행을
 * 제거하지 않고 {@code deleted_at}을 기록한다.
 */
@Entity
@Getter
@Table(name = "tb_attachment", indexes = {
        @Index(name = "idx_attachment_target", columnList = "target_type,target_id,deleted_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {
    /** 첨부파일 메타데이터 고유 식별자. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 첨부파일이 일반 게시판 또는 Q&A 중 어디에 속하는지 나타낸다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AttachmentTargetType targetType;

    /** 첨부파일이 연결된 게시글의 고유 식별자. */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 버킷 안에서 실제 파일을 찾는 중복 불가 객체 키. */
    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    /** 다운로드 시 사용자에게 제공할 원본 파일명. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** 업로드 요청에서 검증한 파일 MIME 타입. */
    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    /** 파일 정책과 화면 표시에 사용하는 바이트 단위 크기. */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** 첨부파일 메타데이터가 최초 등록된 시각. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 명시적으로 삭제된 시각이며 활성 첨부파일은 null이다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 정책 검증과 MinIO 저장을 마친 파일의 메타데이터 엔티티를 생성한다.
     *
     * @param targetType 파일이 첨부된 게시판 유형
     * @param targetId 파일이 첨부된 게시글의 기본 키
     * @param objectKey MinIO 버킷 안에서 실제 파일을 찾는 고유 객체 키
     * @param originalName 사용자가 업로드한 원본 파일명
     * @param contentType 검증을 통과한 MIME 타입
     * @param fileSize 바이트 단위 파일 크기
     * @return 아직 영속화되지 않은 첨부파일 엔티티
     */
    public static Attachment create(AttachmentTargetType targetType, Long targetId,
                                    String objectKey, String originalName,
                                    String contentType, long fileSize) {
        Attachment attachment = new Attachment();
        attachment.targetType = targetType;
        attachment.targetId = targetId;
        attachment.objectKey = objectKey;
        attachment.originalName = originalName;
        attachment.contentType = contentType;
        attachment.fileSize = fileSize;
        return attachment;
    }

    /** 메타데이터에 삭제 시각을 기록하여 이후 활성 첨부파일 조회에서 제외한다. */
    public void softDelete() {
        deletedAt = now();
    }

    /** 최초 저장 직전에 생성 시각을 기록한다. */
    @PrePersist
    private void prePersist() {
        createdAt = now();
    }

    /** DB 정밀도에 맞춘 초 단위 현재 시각을 반환한다. */
    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
