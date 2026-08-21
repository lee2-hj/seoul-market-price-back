package com.seoul.market.seoulmarketprice.token.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import org.springframework.stereotype.Service;

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
public class AdminRefreshTokenService {

    /**
     * Refresh Token 원문을 SHA-256으로 변환한다.
     */
    private final TokenHashService tokenHashService;

    /**
     * Refresh Token 만료시간 설정을 제공한다.
     */
    public AdminRefreshTokenService(TokenHashService tokenHashService) {
        this.tokenHashService = tokenHashService;
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
    public void save(
            Admin admin,
            String rawRefreshToken
    ) {
        admin.replaceRefreshTokenHash(tokenHashService.hash(rawRefreshToken));
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
    public void validate(Admin admin, String rawRefreshToken) {
        if (!tokenHashService.matches(rawRefreshToken, admin.getRefreshTokenHash())) {
            throw new IllegalArgumentException("등록되지 않은 관리자 Refresh Token입니다.");
        }
    }

    /**
     * 관리자 Refresh Token 한 개를 폐기한다.
     *
     * @param rawRefreshToken 폐기할 Refresh Token 원문
     */
    public void revoke(Admin admin, String rawRefreshToken) {
        validate(admin, rawRefreshToken);
        admin.clearRefreshTokenHash();
    }

    /**
     * 특정 관리자의 모든 Refresh Token을 삭제한다.
     *
     * @param adminId 관리자 PK
     */
    public void clear(Admin admin) {
        admin.clearRefreshTokenHash();
    }
}
