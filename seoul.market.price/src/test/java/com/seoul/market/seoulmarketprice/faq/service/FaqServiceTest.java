package com.seoul.market.seoulmarketprice.faq.service;

import com.seoul.market.seoulmarketprice.faq.dto.request.FaqCreateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.request.FaqUpdateRequest;
import com.seoul.market.seoulmarketprice.faq.entity.Faq;
import com.seoul.market.seoulmarketprice.faq.exception.FaqNotFoundException;
import com.seoul.market.seoulmarketprice.faq.repository.FaqRepository;
import com.seoul.market.seoulmarketprice.faq.repository.FaqQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FAQ 서비스의 공개 조회와 관리자 변경 규칙을 검증한다. */
@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock
    private FaqRepository repository;

    @Mock
    private FaqQueryRepository queryRepository;

    private FaqService service;

    @BeforeEach
    void setUp() {
        service = new FaqService(repository, queryRepository);
    }

    /** 공개 목록 조회 시 카테고리를 정규화하고 응답으로 변환한다. */
    @Test
    void getPublicFaqsNormalizesCategory() {
        Faq faq = Faq.create(3L, "질문", "답변", "회원", 1, true);
        when(queryRepository.findPublicList("회원")).thenReturn(List.of(faq));

        var result = service.getPublicFaqs(" 회원 ");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().question()).isEqualTo("질문");
        verify(queryRepository).findPublicList("회원");
    }

    /** 공개 상세 조회 전에 조회수를 직접 증가시킨다. */
    @Test
    void getPublicFaqIncrementsViewCount() {
        Faq faq = Faq.create(3L, "질문", "답변", null, 0, true);
        when(queryRepository.incrementViewCount(1L)).thenReturn(1L);
        when(queryRepository.findPublicById(1L)).thenReturn(Optional.of(faq));

        service.getPublicFaq(1L);

        verify(queryRepository).incrementViewCount(1L);
        verify(queryRepository).findPublicById(1L);
    }

    /** 공개되지 않은 FAQ는 상세 조회 결과가 없는 것으로 처리한다. */
    @Test
    void getPublicFaqRejectsMissingFaq() {
        when(queryRepository.incrementViewCount(1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.getPublicFaq(1L))
                .isInstanceOf(FaqNotFoundException.class);
        verify(queryRepository, never()).findPublicById(1L);
    }

    /** 등록 요청의 선택값이 없으면 정렬 0과 공개 상태를 적용한다. */
    @Test
    void createFaqAppliesDefaultsAndAdminId() {
        when(repository.save(any(Faq.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createFaq(
                7L,
                new FaqCreateRequest(" 질문 ", " 답변 ", null, null, null)
        );

        assertThat(response.question()).isEqualTo("질문");
        assertThat(response.displayOrder()).isZero();
        assertThat(response.visible()).isTrue();
    }

    /** 수정 요청에 입력된 필드와 작업 관리자 인덱스만 반영한다. */
    @Test
    void updateFaqChangesRequestedFields() {
        Faq faq = Faq.create(3L, "기존 질문", "기존 답변", "회원", 1, true);
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(faq));

        var response = service.updateFaq(
                1L,
                9L,
                new FaqUpdateRequest(" 변경 질문 ", null, null, 2, false)
        );

        assertThat(response.question()).isEqualTo("변경 질문");
        assertThat(response.answer()).isEqualTo("기존 답변");
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.visible()).isFalse();
        assertThat(faq.getMemberId()).isEqualTo(9L);
    }

    /** 변경 항목이 없는 수정 요청을 거부한다. */
    @Test
    void updateFaqRejectsEmptyRequest() {
        FaqUpdateRequest request = new FaqUpdateRequest(
                null, null, null, null, null
        );

        assertThatThrownBy(() -> service.updateFaq(1L, 7L, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(queryRepository, never()).findActiveById(1L);
    }

    /** 삭제 시 삭제 시각과 작업 관리자 인덱스를 기록한다. */
    @Test
    void deleteFaqSoftDeletesAndRecordsAdmin() {
        Faq faq = Faq.create(3L, "질문", "답변", null, 0, true);
        when(queryRepository.findActiveById(1L)).thenReturn(Optional.of(faq));

        service.deleteFaq(1L, 8L);

        assertThat(faq.getDeletedAt()).isNotNull();
        assertThat(faq.getMemberId()).isEqualTo(8L);
    }
}
