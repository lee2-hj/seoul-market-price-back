package com.seoul.market.seoulmarketprice.comment.service;

import com.seoul.market.seoulmarketprice.comment.dto.request.CommentCreateRequest;
import com.seoul.market.seoulmarketprice.comment.dto.request.CommentUpdateRequest;
import com.seoul.market.seoulmarketprice.comment.dto.response.CommentResponse;

import java.util.List;

/** 게시판 댓글과 대댓글 기능의 서비스 계약이다. */
public interface CommentService {

    List<CommentResponse> getComments(Long boardId);

    CommentResponse createUserComment(
            Long boardId,
            Long userId,
            CommentCreateRequest request
    );

    CommentResponse createUserReply(
            Long boardId,
            Long parentId,
            Long userId,
            CommentCreateRequest request
    );

    CommentResponse updateUserComment(
            Long boardId,
            Long commentId,
            Long userId,
            CommentUpdateRequest request
    );

    void deleteUserComment(Long boardId, Long commentId, Long userId);

    CommentResponse createAdminComment(
            Long boardId,
            Long adminId,
            CommentCreateRequest request
    );

    CommentResponse changeVisibility(Long commentId, boolean visible);

    void deleteByAdmin(Long commentId);
}
