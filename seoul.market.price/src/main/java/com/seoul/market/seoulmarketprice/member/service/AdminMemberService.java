package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminMemberUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberListResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberPageResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberPasswordResetResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminMemberDetailResponse;
import com.seoul.market.seoulmarketprice.member.exception.MemberNotFoundException;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private final MemberManagementRepository memberManagementRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberPasswordMailSender passwordMailSender;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    public AdminMemberPageResponse getMembers(int page, int size, String keyword) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (normalized != null && normalized.length() > 200) throw new IllegalArgumentException("검색어는 200자 이하이어야 합니다.");
        List<Member> filtered = memberManagementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(member -> !member.isDeleted())
                .filter(member -> normalized == null || matches(member, normalized)).toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<Member> content = filtered.subList(from, to);
        Page<Member> members = new org.springframework.data.domain.PageImpl<>(content, PageRequest.of(page, size), filtered.size());

        return new AdminMemberPageResponse(
                members.getContent().stream().map(AdminMemberListResponse::from).toList(),
                members.getNumber(),
                members.getSize(),
                members.getTotalElements(),
                members.getTotalPages(),
                members.isFirst(),
                members.isLast()
        );
    }

    private boolean matches(Member member, String keyword) {
        String value = keyword.toLowerCase(Locale.ROOT);
        return contains(member.getUserId(), value) || contains(member.getName(), value)
                || contains(member.getEmail(), value) || contains(member.getPhone(), value);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public AdminMemberDetailResponse getMember(Long memberId) {
        Member member = memberManagementRepository.findActiveById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return AdminMemberDetailResponse.from(member);
    }

    @Transactional
    public void updateMember(Long memberId, AdminMemberUpdateRequest request) {
        if (!request.hasChanges()) throw new IllegalArgumentException("수정할 회원 정보가 없습니다.");
        Member member = memberManagementRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
        member.changeAdminEditableProfile(request.zipcode(), request.address(), request.addressDetail(), request.preferredRegion());
    }

    @Transactional
    public AdminMemberPasswordResetResponse resetPassword(Long memberId, String email) {
        Member member = memberManagementRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
        String targetEmail = member.getEmail();
        if (targetEmail == null || targetEmail.isBlank()) {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("비밀번호를 발송할 이메일을 먼저 입력해 주세요.");
            }
            targetEmail = email;
            member.changeContactAndAddress(email, null, null, null);
        }
        String temporaryPassword = generateTemporaryPassword();
        member.changePassword(passwordEncoder.encode(temporaryPassword));
        member.clearRefreshTokenHash();
        passwordMailSender.send(targetEmail, temporaryPassword);
        return new AdminMemberPasswordResetResponse("임시 비밀번호를 이메일로 발송했습니다.");
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberManagementRepository.findActiveByIdForWithdrawal(memberId)
                .orElseThrow(MemberNotFoundException::new);
        member.withdraw();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(8);
        for (int i = 0; i < 8; i++) password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        return password.toString();
    }
}
