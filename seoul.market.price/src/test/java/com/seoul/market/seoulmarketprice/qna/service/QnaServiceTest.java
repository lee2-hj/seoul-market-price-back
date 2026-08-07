package com.seoul.market.seoulmarketprice.qna.service;

import com.seoul.market.seoulmarketprice.qna.dto.request.QnaAnswerRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaCreateRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaUpdateRequest;
import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;
import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import com.seoul.market.seoulmarketprice.qna.exception.QnaAccessDeniedException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaNotFoundException;
import com.seoul.market.seoulmarketprice.qna.repository.QnaQueryRepository;
import com.seoul.market.seoulmarketprice.qna.repository.QnaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QnaServiceTest {
    @Mock
    private QnaRepository repository;

    @Mock
    private QnaQueryRepository queryRepository;

    private QnaService service;

    @BeforeEach
    void setUp() {
        service = new QnaService(repository, queryRepository);
    }

    @Test
    void createQnaAppliesDefaultsAndWriter() {
        when(repository.save(any(QnaBoard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createQna(7L,
                new QnaCreateRequest(" 질문 ", " 내용 ", null, null, null));

        assertThat(response.title()).isEqualTo("질문");
        assertThat(response.questionContent()).isEqualTo("내용");
        assertThat(response.publicQuestion()).isTrue();
        assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WAITING);
    }

    @Test
    void getQnaRejectsInaccessiblePost() {
        when(queryRepository.incrementViewCount(1L, 9L)).thenReturn(0L);

        assertThatThrownBy(() -> service.getQna(1L, 9L))
                .isInstanceOf(QnaNotFoundException.class);
        verify(queryRepository, never()).findAccessibleById(1L, 9L);
    }

    @Test
    void updateQnaRejectsDifferentWriter() {
        QnaBoard qna = QnaBoard.create(3L, "질문", "내용", false, null, null);
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(qna));

        assertThatThrownBy(() -> service.updateQna(1L, 4L,
                new QnaUpdateRequest("변경", null, null, null, null, false)))
                .isInstanceOf(QnaAccessDeniedException.class);
    }

    @Test
    void saveAnswerChangesStatusAndAdmin() {
        QnaBoard qna = QnaBoard.create(3L, "질문", "내용", true, null, null);
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(qna));

        var response = service.saveAnswer(1L, 8L, new QnaAnswerRequest(" 답변 "));

        assertThat(response.answerContent()).isEqualTo("답변");
        assertThat(response.answerStatus()).isEqualTo(AnswerStatus.COMPLETED);
        assertThat(qna.getAnswerMemberId()).isEqualTo(8L);
        assertThat(qna.getAnsweredAt()).isNotNull();
    }

    @Test
    void deleteAnswerReturnsPostToWaiting() {
        QnaBoard qna = QnaBoard.create(3L, "질문", "내용", true, null, null);
        qna.answer(8L, "답변");
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(qna));

        service.deleteAnswer(1L);

        assertThat(qna.getAnswerStatus()).isEqualTo(AnswerStatus.WAITING);
        assertThat(qna.getAnswerContent()).isNull();
        assertThat(qna.getAnswerMemberId()).isNull();
        assertThat(qna.getAnsweredAt()).isNull();
    }

    @Test
    void deleteByAdminSoftDeletesPost() {
        QnaBoard qna = QnaBoard.create(3L, "질문", "내용", true, null, null);
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(qna));

        service.deleteByAdmin(1L);

        assertThat(qna.getDeletedAt()).isNotNull();
    }
}
