package com.seoul.market.seoulmarketprice.qna.dto.response;

import com.seoul.market.seoulmarketprice.board.attachment.dto.AttachmentResponse;

import java.util.List;

/** Q&A 상세 화면에 필요한 데이터를 한 번에 반환한다. */
public record QnaFullDetailResponse(
        QnaDetailResponse detail,
        List<AttachmentResponse> attachments
) {
}
