package com.seoul.market.seoulmarketprice.token.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.token.domain.AdminRefreshToken;
import com.seoul.market.seoulmarketprice.token.repository.AdminRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 관리자 Refresh Token의 저장, 조회 및 폐기를 담당하는 서비스이다.
 *
 * <p>
 * 관리자 Refresh Token 원문은 DB에 직접 저장하지 않는다.
 * {@link TokenHashService}를 사용하여 SHA-256 해시값으로
 * 변환한 뒤 저장한다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AdminRefreshTokenService {

    /**
     * 관리자 Refresh Token의 DB 작업을 담당한다.
     */
    private final AdminRefreshTokenRepository
            adminRefreshTokenRepository;

    /**
     * Refresh Token 원문을 SHA-256으로 변환한다.
     */
    private final TokenHashService tokenHashService;

    /**
     * Refresh Token 만료시간 설정을 제공한다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param adminRefreshTokenRepository 관리자 토큰 Repository
     * @param tokenHashService             토큰 해시 서비스
     * @param jwtProperties                JWT 설정값
     */
    public AdminRefreshTokenService(
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            TokenHashService tokenHashService,
            JwtProperties jwtProperties
    ) {
        this.adminRefreshTokenRepository =
                adminRefreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 새로운 관리자 Refresh Token 정보를 DB에 저장한다.
     *
     * <p>
     * Refresh Token 원문은 저장하지 않고,
     * SHA-256 해시값만 저장한다.
     * </p>
     *
     * @param admin          토큰을 발급받은 관리자
     * @param rawRefreshToken Refresh Token 원문
     */
    @Transactional
    public void save(
            Admin admin,
            String rawRefreshToken
    ) {
        // Refresh Token 원문을 SHA-256 해시값으로 변환한다.
        String tokenHash =
                tokenHashService.hash(rawRefreshToken);

        // 설정된 Refresh Token 유효시간으로 DB 만료 시각을 계산한다.
        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plus(
                                Duration.ofMillis(
                                        jwtProperties
                                                .refreshTokenExpiry()
                                )
                        );

        // 정적 팩토리 메서드로 관리자 토큰 엔티티를 생성한다.
        AdminRefreshToken adminRefreshToken =
                AdminRefreshToken.createAdminRefreshToken(
                        admin,
                        tokenHash,
                        expiresAt
                );

        adminRefreshTokenRepository.save(adminRefreshToken);
    }

    /**
     * 전달받은 관리자 Refresh Token이 사용 가능한지 확인한다.
     *
     * <p>
     * 토큰 원문을 해시로 변환하여 DB에서 조회하고,
     * 폐기 또는 만료 여부를 검사한다.
     * </p>
     *
     * @param rawRefreshToken Refresh Token 원문
     * @return 사용 가능한 관리자 Refresh Token 엔티티
     */
    public AdminRefreshToken getUsableToken(
            String rawRefreshToken
    ) {
        String tokenHash =
                tokenHashService.hash(rawRefreshToken);

        AdminRefreshToken adminRefreshToken =
                adminRefreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "등록되지 않은 관리자 Refresh Token입니다."
                                )
                        );

        if (!adminRefreshToken.isUsable()) {
            throw new IllegalStateException(
                    "만료되었거나 폐기된 관리자 Refresh Token입니다."
            );
        }

        return adminRefreshToken;
    }

    /**
     * 관리자 Refresh Token 한 개를 폐기한다.
     *
     * @param rawRefreshToken 폐기할 Refresh Token 원문
     */
    @Transactional
    public void revoke(String rawRefreshToken) {
        AdminRefreshToken adminRefreshToken =
                getUsableToken(rawRefreshToken);

        adminRefreshToken.revoke();
    }

    /**
     * 특정 관리자의 모든 Refresh Token을 삭제한다.
     *
     * @param adminId 관리자 PK
     */
    @Transactional
    public void deleteAllByAdminId(Long adminId) {
        adminRefreshTokenRepository
                .deleteAllByAdmin_Id(adminId);
    }
}