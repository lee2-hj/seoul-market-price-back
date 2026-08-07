package com.seoul.market.seoulmarketprice.faq.service;

import com.seoul.market.seoulmarketprice.faq.dto.request.FaqCreateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.request.FaqUpdateRequest;
import com.seoul.market.seoulmarketprice.faq.dto.response.AdminFaqResponse;
import com.seoul.market.seoulmarketprice.faq.dto.response.FaqPublicResponse;
import com.seoul.market.seoulmarketprice.faq.entity.Faq;
import com.seoul.market.seoulmarketprice.faq.exception.FaqNotFoundException;
import com.seoul.market.seoulmarketprice.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** FAQ 공개 조회와 관리자 등록·수정·삭제를 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    /** FAQ 저장과 조회를 담당하는 저장소이다. */
    private final FaqRepository faqRepository;

    /** 공개 FAQ를 카테고리와 노출 순서에 맞춰 반환한다. */
    public List<FaqPublicResponse> getPublicFaqs(String category) {
        return faqRepository.findPublicList(normalizeCategory(category)).stream()
                .map(this::toPublicResponse)
                .toList();
    }

    /** 조회수를 먼저 증가시킨 뒤 공개 FAQ 상세를 반환한다. */
    @Transactional
    public FaqPublicResponse getPublicFaq(Long id) {
        if (faqRepository.incrementViewCount(id) == 0) {
            throw new FaqNotFoundException();
        }
        return toPublicResponse(
                faqRepository.findPublicById(id)
                        .orElseThrow(FaqNotFoundException::new)
        );
    }

    /** 노출 여부와 관계없이 활성 FAQ 목록을 반환한다. */
    public List<AdminFaqResponse> getAdminFaqs() {
        return faqRepository.findAdminList().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /** 관리자가 선택한 활성 FAQ 상세를 반환한다. */
    public AdminFaqResponse getAdminFaq(Long id) {
        return toAdminResponse(findActive(id));
    }

    /** 요청의 기본값을 적용하여 FAQ를 등록한다. */
    @Transactional
    public AdminFaqResponse createFaq(Long memberId, FaqCreateRequest request) {
        Faq faq = Faq.create(
                memberId,
                request.question().trim(),
                request.answer().trim(),
                normalizeCategory(request.category()),
                request.displayOrder() == null ? 0 : request.displayOrder(),
                request.visible() == null || request.visible()
        );
        return toAdminResponse(faqRepository.save(faq));
    }

    /** 입력된 필드만 수정하고 작업한 관리자를 갱신한다. */
    @Transactional
    public AdminFaqResponse updateFaq(
            Long id,
            Long memberId,
            FaqUpdateRequest request
    ) {
        validateUpdate(request);
        Faq faq = findActive(id);
        faq.update(
                memberId,
                trim(request.question()),
                trim(request.answer()),
                normalizeCategory(request.category()),
                request.category() != null,
                request.displayOrder(),
                request.visible()
        );
        return toAdminResponse(faq);
    }

    /** FAQ와 삭제 작업을 수행한 관리자 정보를 함께 기록한다. */
    @Transactional
    public void deleteFaq(Long id, Long memberId) {
        findActive(id).softDelete(memberId);
    }

    /** 삭제되지 않은 FAQ를 조회하거나 404 예외를 발생시킨다. */
    private Faq findActive(Long id) {
        return faqRepository.findActiveById(id)
                .orElseThrow(FaqNotFoundException::new);
    }

    /** 수정할 필드가 있고 질문과 답변이 공백이 아닌지 검증한다. */
    private void validateUpdate(FaqUpdateRequest request) {
        if (request.question() == null
                && request.answer() == null
                && request.category() == null
                && request.displayOrder() == null
                && request.visible() == null) {
            throw new IllegalArgumentException("수정할 항목을 하나 이상 입력해야 합니다.");
        }
        if ((request.question() != null && request.question().isBlank())
                || (request.answer() != null && request.answer().isBlank())) {
            throw new IllegalArgumentException("질문과 답변은 공백일 수 없습니다.");
        }
    }

    /** 카테고리 앞뒤 공백을 제거하고 빈 값은 조건 없음으로 변환한다. */
    private String normalizeCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }

    /** null은 유지하고 문자열 앞뒤 공백을 제거한다. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 엔티티를 공개 조회 응답으로 변환한다. */
    private FaqPublicResponse toPublicResponse(Faq faq) {
        return new FaqPublicResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getDisplayOrder(),
                faq.getViewCount(),
                faq.getCreatedAt()
        );
    }

    /** 엔티티를 관리자 조회 응답으로 변환한다. */
    private AdminFaqResponse toAdminResponse(Faq faq) {
        return new AdminFaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getDisplayOrder(),
                faq.isVisible(),
                faq.getViewCount(),
                faq.getCreatedAt(),
                faq.getUpdatedAt()
        );
    }
}
