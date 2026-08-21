package com.seoul.market.seoulmarketprice.token.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import org.springframework.stereotype.Service;
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
public class RefreshTokenService {

    /**
     * Refresh Token 원문을 SHA-256 해시값으로 변환한다.
     */
    private final TokenHashService tokenHashService;

    public RefreshTokenService(TokenHashService tokenHashService) {
        this.tokenHashService = tokenHashService;
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
    public void save(
            Member member,
            String rawRefreshToken
    ) {
        member.replaceRefreshTokenHash(tokenHashService.hash(rawRefreshToken));
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
    public void validate(Member member, String rawRefreshToken) {
        if (!tokenHashService.matches(rawRefreshToken, member.getRefreshTokenHash())) {
            throw new IllegalArgumentException("등록되지 않은 Refresh Token입니다.");
        }
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
    public void revoke(Member member, String rawRefreshToken) {
        validate(member, rawRefreshToken);
        member.clearRefreshTokenHash();
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
    public void clear(Member member) {
        member.clearRefreshTokenHash();
    }
}
