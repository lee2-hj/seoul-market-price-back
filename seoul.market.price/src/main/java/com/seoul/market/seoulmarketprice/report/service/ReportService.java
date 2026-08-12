package com.seoul.market.seoulmarketprice.report.service;

import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.report.dto.request.*;
import com.seoul.market.seoulmarketprice.report.dto.response.*;
import com.seoul.market.seoulmarketprice.report.entity.*;
import com.seoul.market.seoulmarketprice.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/** 신고 조회·접수·권한 확인과 관리자 처리를 수행하는 서비스이다. */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ReportService {
    /** 신고 데이터 저장소. */
    private final ReportRepository reportRepository;
    /** 신고 작성 회원의 활성 상태를 확인하는 저장소. */
    private final MemberManagementRepository memberRepository;

    /** 검색 조건을 적용한 신고 목록을 최신순 페이지로 반환한다. */
    public ReportPageResponse getReports(int page, int size, ReportCategory category, String keyword) {
        validatePage(page, size);
        String normalizedKeyword = normalize(keyword);
        Page<Report> result = reportRepository.findPublicPage(category, normalizedKeyword,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new ReportPageResponse(result.getContent().stream().map(this::toList).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    /** 비밀글 접근 권한을 확인한 뒤 신고 상세 정보를 반환한다. */
    public ReportDetailResponse getReport(Long id, Long memberId, boolean admin) {
        Report report = findActive(id);
        requireAccessible(report, memberId, admin);
        return toDetail(report);
    }

    /** 활성 회원의 신고를 접수하고 최초 상태를 {@code RECEIVED}로 저장한다. */
    @Transactional
    public ReportDetailResponse createReport(Long memberId, ReportCreateRequest request) {
        // JWT에 남은 탈퇴 회원이나 존재하지 않는 회원은 신고자로 사용할 수 없다.
        var member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new AccessDeniedException("가입된 회원만 신고할 수 있습니다."));
        Report report = Report.create(memberId, request.category(), required(request.targetProperty()),
                required(request.title()), required(request.content()), request.isSecret());
        Report saved = reportRepository.save(report);
        return new ReportDetailResponse(saved.getId(), saved.getCategory(), saved.getStatus(),
                saved.getTargetProperty(), saved.getTitle(), saved.getContent(), maskName(member.getName()),
                saved.getUserId(), saved.isSecret(), null, null, null,
                saved.getCreatedAt(), saved.getUpdatedAt());
    }

    /** 작성자 본인의 신고만 소프트 삭제한다. */
    @Transactional
    public void deleteByOwner(Long id, Long memberId) {
        Report report = findActive(id);
        if (!report.isOwnedBy(memberId)) throw new AccessDeniedException("신고 작성자만 삭제할 수 있습니다.");
        report.softDelete();
    }

    /** 관리자가 신고 상태 또는 답변을 변경한다. */
    @Transactional
    public ReportDetailResponse updateByAdmin(Long id, Long adminId, ReportAdminUpdateRequest request) {
        if (request.status() == null && request.replyContent() == null) throw new IllegalArgumentException("변경할 항목이 없습니다.");
        String reply = request.replyContent() == null ? null : required(request.replyContent());
        Report report = findActive(id);
        report.updateByAdmin(adminId, request.status(), reply);
        return toDetail(report);
    }

    /** 관리자가 활성 신고를 소프트 삭제한다. */
    @Transactional public void deleteByAdmin(Long id) { findActive(id).softDelete(); }
    /** 첨부파일 작업 전에 요청 회원이 신고 작성자인지 확인한다. */
    public void requireOwner(Long id, Long memberId) {
        if (!findActive(id).isOwnedBy(memberId)) throw new AccessDeniedException("신고 작성자만 접근할 수 있습니다.");
    }
    /** 비밀 신고의 상세·첨부파일 접근 권한을 확인한다. */
    public void requireAccessible(Long id, Long memberId, boolean admin) { requireAccessible(findActive(id), memberId, admin); }
    /** 관리자 작업 전에 신고가 활성 상태인지 확인한다. */
    public void requireActive(Long id) { findActive(id); }

    /** 비밀 신고는 작성자와 관리자만 접근하도록 제한한다. */
    private void requireAccessible(Report report, Long memberId, boolean admin) {
        if (report.isSecret() && !admin && !report.isOwnedBy(memberId)) throw new AccessDeniedException("비공개 신고입니다.");
    }
    /** 삭제되지 않은 신고를 조회하고 없으면 예외를 발생시킨다. */
    private Report findActive(Long id) { return reportRepository.findActiveById(id).orElseThrow(() -> new NoSuchElementException("신고글을 찾을 수 없습니다.")); }
    /** 엔티티를 목록 응답으로 변환하며 비밀 신고 내용을 숨긴다. */
    private ReportListResponse toList(Report r) {
        return new ReportListResponse(r.getId(), r.getCategory(), r.getStatus(), r.isSecret() ? null : r.getTargetProperty(),
                r.isSecret() ? null : r.getTitle(), maskName(r.getUser().getName()), r.isSecret(), r.getCreatedAt());
    }
    /** 엔티티를 작성자와 관리자 정보를 포함한 상세 응답으로 변환한다. */
    private ReportDetailResponse toDetail(Report r) {
        return new ReportDetailResponse(r.getId(), r.getCategory(), r.getStatus(), r.getTargetProperty(), r.getTitle(),
                r.getContent(), maskName(r.getUser().getName()), r.getUserId(), r.isSecret(), r.getAdminReply(),
                r.getAdmin() == null ? null : r.getAdmin().getName(), r.getRepliedAt(), r.getCreatedAt(), r.getUpdatedAt());
    }
    /** 작성자 개인정보 노출을 줄이기 위해 이름 가운데 부분을 마스킹한다. */
    private String maskName(String name) {
        if (name == null || name.isBlank()) return "익명";
        String value = name.trim();
        if (value.length() == 1) return value;
        if (value.length() == 2) return value.charAt(0) + "*";
        return value.charAt(0) + "*" + value.substring(2);
    }
    /** 필수 문자열을 정규화하고 빈 값이면 요청 오류로 처리한다. */
    private String required(String value) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException("필수 값을 입력해주세요.");
        return normalized;
    }
    /** 문자열의 공백을 제거하고 빈 문자열은 {@code null}로 변환한다. */
    private String normalize(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    /** 허용하는 페이지 번호와 크기 범위를 검증한다. */
    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("올바르지 않은 페이지 요청입니다.");
    }
}
