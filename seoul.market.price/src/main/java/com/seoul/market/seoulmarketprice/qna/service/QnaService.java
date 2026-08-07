package com.seoul.market.seoulmarketprice.qna.service;

import com.seoul.market.seoulmarketprice.qna.dto.request.QnaAnswerRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaCreateRequest;
import com.seoul.market.seoulmarketprice.qna.dto.request.QnaUpdateRequest;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaDetailResponse;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaListResponse;
import com.seoul.market.seoulmarketprice.qna.dto.response.QnaPageResponse;
import com.seoul.market.seoulmarketprice.qna.entity.AnswerStatus;
import com.seoul.market.seoulmarketprice.qna.entity.QnaBoard;
import com.seoul.market.seoulmarketprice.qna.exception.QnaAccessDeniedException;
import com.seoul.market.seoulmarketprice.qna.exception.QnaNotFoundException;
import com.seoul.market.seoulmarketprice.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Q&A의 조회, 작성자 권한 검증, 질문 관리와 관리자 답변 업무를 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {
    /** Q&A 영속성과 화면별 검색을 담당하는 저장소이다. */
    private final QnaRepository repository;

    /** 공개 Q&A 목록을 검색 조건과 페이지 정보에 맞춰 조회한다. */
    public QnaPageResponse getPublicQnas(int page, int size, String keyword, AnswerStatus status) {
        validatePage(page, size);
        Page<QnaBoard> result = repository.findPublicPage(normalize(keyword), status, pageable(page, size));
        return toPageResponse(result);
    }

    /** 로그인 사용자가 작성한 공개·비공개 Q&A 목록을 조회한다. */
    public QnaPageResponse getMyQnas(Long userId, int page, int size, String keyword, AnswerStatus status) {
        validatePage(page, size);
        Page<QnaBoard> result = repository.findMyPage(userId, normalize(keyword), status, pageable(page, size));
        return toPageResponse(result);
    }

    /** 접근 가능한 Q&A의 조회수를 증가시키고 상세 정보를 반환한다. */
    @Transactional
    public QnaDetailResponse getQna(Long id, Long userId) {
        if (repository.incrementViewCount(id, userId) == 0) {
            throw new QnaNotFoundException();
        }
        return toDetailResponse(repository.findAccessibleById(id, userId)
                .orElseThrow(QnaNotFoundException::new));
    }

    /** 백오피스 검색 조건과 작성 기간을 적용해 관리자용 목록을 조회한다. */
    public QnaPageResponse getAdminQnas(int page, int size, String keyword, AnswerStatus status,
                                        Boolean publicQuestion, String writer, LocalDate from, LocalDate to) {
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        Page<QnaBoard> result = repository.findAdminPage(
                normalize(keyword), status, publicQuestion, normalize(writer),
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                pageable(page, size));
        return toPageResponse(result);
    }

    /** 삭제되지 않은 Q&A 상세를 관리자 권한으로 조회한다. */
    public QnaDetailResponse getAdminQna(Long id) {
        return toDetailResponse(findActive(id));
    }

    /** 로그인 사용자 식별자를 작성자로 사용해 신규 질문을 등록한다. */
    @Transactional
    public QnaDetailResponse createQna(Long userId, QnaCreateRequest request) {
        QnaBoard saved = repository.save(QnaBoard.create(
                userId, required(request.title(), "제목"), required(request.questionContent(), "질문 내용"),
                request.publicQuestion() == null || request.publicQuestion(),
                normalize(request.attachName()), normalize(request.attachPath())));
        return toDetailResponse(saved);
    }

    /** 작성자 확인 후 전달된 질문 필드만 부분 수정한다. */
    @Transactional
    public QnaDetailResponse updateQna(Long id, Long userId, QnaUpdateRequest request) {
        validateUpdate(request);
        QnaBoard qna = findActive(id);
        if (!qna.isOwnedBy(userId)) {
            throw new QnaAccessDeniedException();
        }
        qna.updateQuestion(optionalRequired(request.title(), "제목"),
                optionalRequired(request.questionContent(), "질문 내용"), request.publicQuestion(),
                normalize(request.attachName()), normalize(request.attachPath()),
                Boolean.TRUE.equals(request.attachmentChanged()));
        return toDetailResponse(qna);
    }

    /** 작성자 확인 후 질문을 소프트 삭제한다. */
    @Transactional
    public void deleteQna(Long id, Long userId) {
        QnaBoard qna = findActive(id);
        if (!qna.isOwnedBy(userId)) {
            throw new QnaAccessDeniedException();
        }
        qna.softDelete();
    }

    /** 관리자 답변을 등록하거나 기존 답변을 덮어쓰고 답변완료 상태로 변경한다. */
    @Transactional
    public QnaDetailResponse saveAnswer(Long id, Long adminId, QnaAnswerRequest request) {
        QnaBoard qna = findActive(id);
        qna.answer(adminId, required(request.answerContent(), "답변 내용"));
        return toDetailResponse(qna);
    }

    /** 관리자 답변을 제거하고 질문을 답변대기 상태로 되돌린다. */
    @Transactional
    public void deleteAnswer(Long id) {
        findActive(id).removeAnswer();
    }

    /** 관리자가 질문의 공개 여부를 변경한다. */
    @Transactional
    public QnaDetailResponse changeVisibility(Long id, boolean publicQuestion) {
        QnaBoard qna = findActive(id);
        qna.changeVisibility(publicQuestion);
        return toDetailResponse(qna);
    }

    /** 관리자가 질문을 소프트 삭제한다. */
    @Transactional
    public void deleteByAdmin(Long id) {
        findActive(id).softDelete();
    }

    /** 삭제되지 않은 Q&A를 조회하고 없으면 공통 예외를 발생시킨다. */
    private QnaBoard findActive(Long id) {
        return repository.findActiveById(id).orElseThrow(QnaNotFoundException::new);
    }

    /** 최신 등록순 정렬을 적용한 페이지 요청 객체를 생성한다. */
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    /** JPA 페이지 결과를 API 페이지 응답으로 변환한다. */
    private QnaPageResponse toPageResponse(Page<QnaBoard> page) {
        return new QnaPageResponse(page.getContent().stream().map(this::toListResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }

    /** Q&A 엔티티를 목록용 요약 응답으로 변환한다. */
    private QnaListResponse toListResponse(QnaBoard qna) {
        return new QnaListResponse(qna.getId(), qna.getTitle(), qna.getWriterLoginId(), qna.getWriterName(),
                qna.getAnswerStatus(), qna.getViewCount(), qna.isPublicQuestion(),
                qna.getAttachName() != null, qna.getCreatedAt(), qna.getAnsweredAt());
    }

    /** Q&A 엔티티를 질문·답변 상세 응답으로 변환한다. */
    private QnaDetailResponse toDetailResponse(QnaBoard qna) {
        return new QnaDetailResponse(qna.getId(), qna.getWriterLoginId(), qna.getWriterName(), qna.getTitle(),
                qna.getQuestionContent(), qna.getAnswerContent(), qna.getAnswerAdminName(),
                qna.getAnswerStatus(), qna.getAttachName(), qna.getViewCount(), qna.isPublicQuestion(),
                qna.getCreatedAt(), qna.getUpdatedAt(), qna.getAnsweredAt());
    }

    /** 허용된 페이지 번호와 크기 범위를 검증한다. */
    private void validatePage(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
    }

    /** 부분 수정 요청에 실제 변경 항목이 하나 이상 있는지 검증한다. */
    private void validateUpdate(QnaUpdateRequest request) {
        if (request.title() == null && request.questionContent() == null && request.publicQuestion() == null
                && !Boolean.TRUE.equals(request.attachmentChanged())) {
            throw new IllegalArgumentException("수정할 항목이 없습니다.");
        }
    }

    /** 필수 문자열의 앞뒤 공백을 제거하고 빈 값이면 요청 오류를 발생시킨다. */
    private String required(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(field + "을(를) 입력해 주세요.");
        return normalized;
    }

    /** 선택 문자열이 전달된 경우에만 필수 문자열 규칙을 적용한다. */
    private String optionalRequired(String value, String field) {
        return value == null ? null : required(value, field);
    }

    /** 문자열 앞뒤 공백을 제거하고 빈 문자열을 null로 정규화한다. */
    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
