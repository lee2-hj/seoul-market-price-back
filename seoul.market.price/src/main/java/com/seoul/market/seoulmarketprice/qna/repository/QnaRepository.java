package com.seoul.market.seoulmarketprice.qna.repository;

import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import org.springframework.data.jpa.repository.JpaRepository;

/** Q&A 게시글의 저장과 기본 CRUD를 담당한다. */
public interface QnaRepository extends JpaRepository<QnaBoard, Long> {
}
