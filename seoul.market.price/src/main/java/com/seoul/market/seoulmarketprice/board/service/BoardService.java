package com.seoul.market.seoulmarketprice.board.service;
import com.seoul.market.seoulmarketprice.board.dto.request.AdminBoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.BoardUpdateRequest;
import com.seoul.market.seoulmarketprice.board.dto.request.NoticeCreateRequest;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardDetailResponse;
import com.seoul.market.seoulmarketprice.board.dto.response.BoardPageResponse;

/** 일반 게시판 기능의 서비스 계약이다. */
public interface BoardService {

    BoardPageResponse getBoards(int page, int size, String keyword);

    BoardDetailResponse getBoard(Long id);

    BoardDetailResponse createBoard(Long userId, BoardCreateRequest request);

    BoardDetailResponse updateBoard(Long id, Long userId, BoardUpdateRequest request);

    void deleteBoard(Long id, Long userId);

    BoardDetailResponse createNotice(Long adminId, NoticeCreateRequest request);

    BoardDetailResponse updateByAdmin(Long id, AdminBoardUpdateRequest request);

    void deleteByAdmin(Long id);
}
