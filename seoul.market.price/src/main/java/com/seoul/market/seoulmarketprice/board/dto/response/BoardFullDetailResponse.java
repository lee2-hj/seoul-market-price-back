package com.seoul.market.seoulmarketprice.board.dto.response;

import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentResponse;
import com.seoul.market.seoulmarketprice.board.comment.dto.response.CommentResponse;

import java.util.List;

/** 게시글 상세 화면에 필요한 데이터를 한 번에 반환한다. */
public record BoardFullDetailResponse(
        BoardDetailResponse detail,
        List<CommentResponse> comments,
        List<AttachmentResponse> attachments
) {
}
