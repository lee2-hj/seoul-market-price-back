package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import java.util.Optional;
import java.util.List;

public interface MemberManagementRepositoryCustom {
    boolean existsActiveByUserId(String userId);
    boolean existsActiveByPhone(String phoneNumber);
    boolean existsActiveByCi(String ci);
    boolean existsAnyByCi(String ci);
    boolean existsActiveByNameAndPhone(String name, String phone);
    Optional<Member> findActiveLocalByUserIdForCiRegistration(String userId);
    Optional<Member> findActiveByIdForPasswordReset(Long memberId);
    Optional<Member> findActiveById(Long memberId);
    Optional<Member> findActiveByIdForUpdate(Long memberId);
    Optional<Member> findActiveByIdForWithdrawal(Long memberId);
    List<Member> findActiveLocalMembersByVerifiedIdentity(String name, String phone);
}
