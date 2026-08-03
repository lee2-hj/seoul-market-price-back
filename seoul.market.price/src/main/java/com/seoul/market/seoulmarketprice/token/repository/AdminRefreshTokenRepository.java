package com.seoul.market.seoulmarketprice.token.repository;

import com.seoul.market.seoulmarketprice.token.domain.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 Refresh Token의 저장과 조회를 담당하는 Repository이다.
 *
 * <p>
 * 일반 회원의 Refresh Token Repository와 분리하여
 * 관리자 토큰이 회원 토큰 저장소에 섞이지 않도록 한다.
 * </p>
 */
public interface AdminRefreshTokenRepository
        extends JpaRepository<AdminRefreshToken, Long> {

    /**
     * Refresh Token 해시값으로 관리자 토큰을 조회한다.
     *
     * @param tokenHash SHA-256으로 변환된 Refresh Token 해시값
     * @return 조회된 관리자 Refresh Token
     */
    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    /**
     * 관리자 PK로 해당 관리자의 모든 Refresh Token을 조회한다.
     *
     * @param adminId 관리자 PK
     * @return 관리자가 발급받은 Refresh Token 목록
     */
    List<AdminRefreshToken> findAllByAdmin_Id(Long adminId);

    /**
     * 관리자 PK로 해당 관리자의 모든 Refresh Token을 삭제한다.
     *
     * <p>
     * 모든 기기에서 로그아웃시키거나 관리자 계정을 삭제할 때
     * 사용할 수 있다.
     * </p>
     *
     * @param adminId 관리자 PK
     */
    void deleteAllByAdmin_Id(Long adminId);
}