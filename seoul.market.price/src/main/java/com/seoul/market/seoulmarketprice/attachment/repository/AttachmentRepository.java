package com.seoul.market.seoulmarketprice.attachment.repository;

import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 첨부파일 메타데이터의 JPA 저장과 소프트 삭제 조건이 적용된 활성 파일 조회를 담당한다. */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    /**
     * 특정 게시글에 연결된 활성 첨부파일을 등록 순서대로 조회한다.
     *
     * @param targetType 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글 기본 키
     * @return {@code deleted_at IS NULL}인 첨부파일 목록
     */
    List<Attachment> findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByIdAsc(
            AttachmentTargetType targetType, Long targetId
    );

    @Query("""
            select distinct attachment.targetId
            from Attachment attachment
            where attachment.targetType = :targetType
              and attachment.targetId in :targetIds
              and attachment.deletedAt is null
            """)
    List<Long> findActiveTargetIds(
            @Param("targetType") AttachmentTargetType targetType,
            @Param("targetIds") List<Long> targetIds
    );

    /**
     * 게시판 유형·게시글 ID·첨부파일 ID가 모두 일치하는 활성 파일을 조회한다.
     * 다른 게시글의 첨부파일 ID를 URL에 넣어 접근하는 것을 서비스 계층에서 차단할 수 있게 한다.
     *
     * @param id 첨부파일 기본 키
     * @param targetType 첨부 대상 게시판 유형
     * @param targetId 첨부 대상 게시글 기본 키
     * @return 조건에 맞는 활성 첨부파일, 없으면 빈 값
     */
    Optional<Attachment> findByIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
            Long id, AttachmentTargetType targetType, Long targetId
    );
}
