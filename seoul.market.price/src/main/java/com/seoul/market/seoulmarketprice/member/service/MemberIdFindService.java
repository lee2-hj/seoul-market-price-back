package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdFindRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdFindResponse;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** PASS에서 확인한 이름과 전화번호를 이용해 일반 회원 아이디를 찾는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberIdFindService {

    private final PhoneVerificationService phoneVerificationService;
    private final MemberManagementRepository memberManagementRepository;

    public MemberIdFindResponse find(MemberIdFindRequest request) {
        PhoneVerificationConfirmResponse verification =
                phoneVerificationService.confirm(
                        new PhoneVerificationConfirmRequest(
                                request.identityVerificationId()
                        )
                );

        String normalizedName = verification.name().trim();
        String normalizedPhone = normalizePhone(verification.phoneNumber());

        List<String> maskedUserIds = memberManagementRepository
                .findActiveLocalMembersByVerifiedIdentity(
                        normalizedName,
                        normalizedPhone
                )
                .stream()
                .map(Member::getUserId)
                .map(MemberIdFindService::maskUserId)
                .toList();

        return new MemberIdFindResponse(
                !maskedUserIds.isEmpty(),
                maskedUserIds
        );
    }

    static String normalizePhone(String phoneNumber) {
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("82")) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    static String maskUserId(String userId) {
        int length = userId.length();
        if (length <= 2) {
            return "*".repeat(length);
        }
        if (length == 3) {
            return userId.substring(0, 1) + "**";
        }
        if (length == 4) {
            return userId.substring(0, 1) + "**" + userId.substring(3);
        }
        if (length == 5) {
            return userId.substring(0, 2) + "**" + userId.substring(4);
        }
        return userId.substring(0, 3)
                + "****"
                + userId.substring(length - 2);
    }
}
