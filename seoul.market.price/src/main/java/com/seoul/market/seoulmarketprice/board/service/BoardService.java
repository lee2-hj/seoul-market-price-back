package com.seoul.market.seoulmarketprice.board.service;
import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.NoticeCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardPageResponse;

/** 일반 게시판 기능의 서비스 계약이다. */
public interface BoardService {

    /** 공개 게시글 목록을 검색 조건과 페이지 정보에 맞춰 조회한다. */
    BoardPageResponse getBoards(int page, int size, String keyword);

    /** 공개 게시글 상세 정보를 조회하고 조회수를 증가시킨다. */
    BoardDetailResponse getBoard(Long id);

    /** 로그인한 일반 사용자 명의로 게시글을 작성한다. */
    BoardDetailResponse createBoard(Long userId, String loginId, BoardCreateRequest request);

    /** 작성자 본인의 일반 게시글 제목과 내용을 수정한다. */
    BoardDetailResponse updateBoard(Long id, Long userId, BoardUpdateRequest request);

    /** 작성자 본인의 일반 게시글을 소프트 삭제한다. */
    void deleteBoard(Long id, Long userId);

    /** 로그인한 관리자의 공지사항을 작성한다. */
    BoardDetailResponse createNotice(Long adminId, NoticeCreateRequest request);

    /** 관리자가 게시글의 내용과 노출·고정 상태를 수정한다. */
    BoardDetailResponse updateByAdmin(Long id, AdminBoardUpdateRequest request);

    /** 관리자가 지정한 게시글을 소프트 삭제한다. */
    void deleteByAdmin(Long id);
}
