package com.seoul.market.seoulmarketprice.token.repository;

import com.seoul.market.seoulmarketprice.token.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Refresh Token의 저장과 조회를 담당하는 Repository이다.
 */
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    /**
     * Refresh Token 해시값으로 토큰을 조회한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 회원 PK로 해당 회원의 모든 Refresh Token을 조회한다.
     */
    List<RefreshToken> findAllByMember_Id(Long memberId);

    /**
     * 회원 PK로 해당 회원의 모든 Refresh Token을 삭제한다.
     */
    void deleteAllByMember_Id(Long memberId);
}