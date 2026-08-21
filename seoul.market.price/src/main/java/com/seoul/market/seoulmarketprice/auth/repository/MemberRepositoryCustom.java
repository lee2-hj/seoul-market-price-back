package com.seoul.market.seoulmarketprice.auth.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import java.util.Optional;

public interface MemberRepositoryCustom {
    Optional<Member> findActiveByUserId(String userId);
    boolean existsActiveById(Long id);
    Optional<Member> findActiveById(Long id);
    Optional<Member> findActiveByIdForTokenUpdate(Long id);
}
