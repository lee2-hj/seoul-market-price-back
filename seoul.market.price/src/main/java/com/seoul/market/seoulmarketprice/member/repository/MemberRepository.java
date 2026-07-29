package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 Repository
 *
 * Member 엔티티와 DB(tb_user)를 연결하는 역할을 한다.
 *
 * Repository는 DB와 직접 통신하는 계층이다.
 *
 * Service에서는 Repository를 통해 회원을 조회하거나 저장한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 로그인 아이디(user_id)로 회원을 조회한다.
     *
     * SQL로 표현하면
     *
     * SELECT *
     * FROM tb_user
     * WHERE user_id = ?
     *
     * 와 같은 의미이다.
     *
     * Optional을 사용하는 이유
     * 회원이 없을 수도 있기 때문이다.
     */
    Optional<Member> findByUserId(String userId);

    Optional<Member> findBySocialId(String socialId);

}