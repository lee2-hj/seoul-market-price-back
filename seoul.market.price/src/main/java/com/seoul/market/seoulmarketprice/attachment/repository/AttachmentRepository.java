package com.seoul.market.seoulmarketprice.attachment.repository;

import com.seoul.market.seoulmarketprice.attachment.entity.Attachment;
import com.seoul.market.seoulmarketprice.attachment.entity.AttachmentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 첨부파일 메타데이터의 저장과 활성 파일 조회를 담당한다. */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    /** 특정 게시글에 연결된 활성 첨부파일을 등록 순서대로 조회한다. */
    List<Attachment> findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByIdAsc(
            AttachmentTargetType targetType, Long targetId
    );

    /** 게시판 종류, 게시글, 첨부파일 식별자가 모두 일치하는 활성 파일을 조회한다. */
    Optional<Attachment> findByIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
            Long id, AttachmentTargetType targetType, Long targetId
    );
}
