package com.seoul.market.seoulmarketprice.token.domain;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자의 Refresh Token 저장 정보를 관리하는 엔티티이다.
 *
 * <p>
 * 일반 회원의 Refresh Token과 관리자의 Refresh Token을
 * 완전히 분리하기 위해 {@code tb_admin_refresh_token} 테이블을 사용한다.
 * </p>
 *
 * <p>
 * 실제 Refresh Token 원문은 DB에 저장하지 않고,
 * SHA-256으로 변환한 해시값만 저장한다.
 * </p>
 *
 * <p>
 * 관리자 로그인, Refresh Token Rotation,
 * 로그아웃 과정에서 사용된다.
 * </p>
 */
@Entity
@Getter
@Table(name = "tb_admin_refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefreshToken {

    /**
     * 관리자 Refresh Token 데이터의 고유번호이다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Refresh Token을 발급받은 관리자이다.
     *
     * <p>
     * 하나의 관리자가 여러 브라우저나 기기에서 로그인할 수 있도록
     * 관리자와 Refresh Token을 다대일 관계로 연결한다.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    /**
     * 관리자 Refresh Token 원문의 SHA-256 해시값이다.
     *
     * <p>
     * 토큰 해시는 64자리 16진수 문자열이며,
     * 같은 토큰이 중복 저장되지 않도록 unique 제약조건을 적용한다.
     * </p>
     */
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    /**
     * Refresh Token의 만료 시각이다.
     *
     * <p>
     * JWT 자체의 만료 시간과 별도로 DB에서도 만료 시간을 검사한다.
     * </p>
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Refresh Token의 폐기 여부이다.
     *
     * <p>
     * Rotation 또는 로그아웃이 수행되면 {@code true}로 변경된다.
     * </p>
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /**
     * 새로운 관리자 Refresh Token 저장 객체를 생성한다.
     *
     * <p>
     * Setter나 Builder를 사용하지 않고,
     * 의미가 명확한 정적 팩토리 메서드로 생성한다.
     * </p>
     *
     * @param admin     토큰을 발급받은 관리자
     * @param tokenHash Refresh Token의 SHA-256 해시값
     * @param expiresAt Refresh Token의 만료 시각
     * @return 생성된 관리자 Refresh Token 엔티티
     */
    public static AdminRefreshToken createAdminRefreshToken(
            Admin admin,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        AdminRefreshToken adminRefreshToken =
                new AdminRefreshToken();

        adminRefreshToken.admin = admin;
        adminRefreshToken.tokenHash = tokenHash;
        adminRefreshToken.expiresAt = expiresAt;
        adminRefreshToken.revoked = false;

        return adminRefreshToken;
    }

    /**
     * 관리자 Refresh Token을 폐기한다.
     *
     * <p>
     * Rotation이나 로그아웃 이후에는
     * 해당 토큰을 다시 사용할 수 없다.
     * </p>
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * 현재 Refresh Token을 사용할 수 있는지 확인한다.
     *
     * @return 폐기되지 않았고 만료되지 않았다면 {@code true}
     */
    public boolean isUsable() {
        return !revoked
                && expiresAt.isAfter(LocalDateTime.now());
    }
}