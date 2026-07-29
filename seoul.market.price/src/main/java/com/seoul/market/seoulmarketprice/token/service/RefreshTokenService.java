package com.seoul.market.seoulmarketprice.token.service;

import com.seoul.market.seoulmarketprice.member.domain.Member;
import com.seoul.market.seoulmarketprice.security.jwt.JwtProperties;
import com.seoul.market.seoulmarketprice.token.domain.RefreshToken;
import com.seoul.market.seoulmarketprice.token.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
/**
 * Refresh Token의 저장, 조회, 폐기를 담당하는 서비스이다.
 *
 * <p>
 * Refresh Token 원문은 DB에 직접 저장하지 않는다.
 * {@link TokenHashService}를 이용해 SHA-256 해시값으로 변환한 뒤 저장한다.
 * </p>
 *
 * <p>
 * 로그인, 토큰 재발급, 로그아웃 과정에서 사용된다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    /**
     * Refresh Token DB 작업을 담당하는 Repository이다.
     */
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh Token 원문을 SHA-256 해시값으로 변환한다.
     */
    private final TokenHashService tokenHashService;

    /**
     * Refresh Token의 만료시간 설정값을 가지고 있다.
     */
    private final JwtProperties jwtProperties;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param refreshTokenRepository Refresh Token Repository
     * @param tokenHashService       토큰 해시 변환 서비스
     * @param jwtProperties          JWT 설정값
     */
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenHashService tokenHashService,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 새 Refresh Token 정보를 DB에 저장한다.
     *
     * <p>
     * 토큰 원문은 쿠키에 전달하고,
     * DB에는 SHA-256 해시값만 저장한다.
     * </p>
     *
     * @param member          토큰을 발급받은 회원
     * @param rawRefreshToken 실제 Refresh Token 문자열
     */
    @Transactional
    public void save(
            Member member,
            String rawRefreshToken
    ) {
        /*
         * Refresh Token 원문을 SHA-256 해시값으로 변환한다.
         */
        String tokenHash = tokenHashService.hash(rawRefreshToken);

        /*
         * 현재 시각에 설정된 Refresh Token 유효시간을 더해
         * DB에서 관리할 만료 시각을 계산한다.
         */
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(
                        Duration.ofMillis(
                                jwtProperties.refreshTokenExpiry()
                        )
                );

        /*
         * Setter 대신 RefreshToken의 정적 팩토리 메서드를 사용해
         * 새로운 엔티티를 생성한다.
         */
        RefreshToken refreshToken = RefreshToken.create(
                member,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 전달받은 Refresh Token이 사용 가능한지 검사하고 조회한다.
     *
     * <p>
     * 쿠키에서 받은 토큰을 해시로 변환한 뒤 DB에서 조회한다.
     * 토큰이 없거나 폐기되었거나 만료되었다면 예외를 발생시킨다.
     * </p>
     *
     * @param rawRefreshToken 쿠키에서 전달받은 Refresh Token 원문
     * @return 사용 가능한 RefreshToken 엔티티
     */
    public RefreshToken getUsableToken(String rawRefreshToken) {
        String tokenHash = tokenHashService.hash(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "등록되지 않은 Refresh Token입니다."
                ));

        if (!refreshToken.isUsable()) {
            throw new IllegalStateException(
                    "만료되었거나 폐기된 Refresh Token입니다."
            );
        }

        return refreshToken;
    }

    /**
     * Refresh Token 한 개를 폐기한다.
     *
     * <p>
     * 토큰 재발급 시 기존 토큰을 사용할 수 없게 만들거나,
     * 현재 기기에서 로그아웃할 때 사용한다.
     * </p>
     *
     * @param rawRefreshToken 폐기할 Refresh Token 원문
     */
    @Transactional
    public void revoke(String rawRefreshToken) {
        RefreshToken refreshToken =
                getUsableToken(rawRefreshToken);

        refreshToken.revoke();
    }

    /**
     * 특정 회원의 모든 Refresh Token을 삭제한다.
     *
     * <p>
     * 모든 기기에서 로그아웃시키는 기능에 사용할 수 있다.
     * </p>
     *
     * @param memberId 회원 고유번호
     */
    @Transactional
    public void deleteAllByMemberId(Long memberId) {
        refreshTokenRepository.deleteAllByMember_Id(memberId);
    }
}