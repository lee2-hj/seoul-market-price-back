package com.seoul.market.seoulmarketprice.signup.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupMemberRepository extends JpaRepository<Member, Long> {
}
