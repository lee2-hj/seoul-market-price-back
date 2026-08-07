package com.seoul.market.seoulmarketprice.board.repository;

import com.seoul.market.seoulmarketprice.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글의 저장과 기본 CRUD를 담당한다. */
public interface BoardRepository extends JpaRepository<Board, Long> {
}
