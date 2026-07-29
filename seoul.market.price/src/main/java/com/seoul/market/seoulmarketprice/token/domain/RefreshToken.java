package com.seoul.market.seoulmarketprice.token.domain;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
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
 * Refresh Token의 저장 정보를 관리하는 엔티티이다.
 *
 * <p>
 * 실제 Refresh Token 문자열을 그대로 저장하지 않고,
 * SHA-256으로 변환한 해시값을 저장할 예정이다.
 * </p>
 *
 * <p>
 * 토큰 원문이 아닌 해시값을 저장하면
 * DB 정보가 노출되어도 Refresh Token을 그대로 사용할 수 없다.
 * </p>
 *
 * <p>
 * Refresh Token Rotation 과정에서 기존 토큰을 폐기하고
 * 새로운 토큰을 발급할 때 사용한다.
 * </p>
 */
@Entity
@Getter
@Table(name = "tb_refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    /**
     * Refresh Token 데이터의 고유 번호이다.
     *
     * <p>
     * DB에서 AUTO_INCREMENT 방식으로 생성된다.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Refresh Token을 발급받은 회원이다.
     *
     * <p>
     * 여러 기기 또는 여러 브라우저의 로그인을 지원할 수 있도록
     * 한 회원이 여러 Refresh Token을 가질 수 있게 ManyToOne으로 연결한다.
     * </p>
     *
     * <p>
     * LAZY 방식을 사용하여 회원 정보가 실제로 필요할 때만 조회한다.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * Refresh Token 원문을 SHA-256으로 변환한 해시값이다.
     *
     * <p>
     * 같은 토큰 해시가 중복 저장되지 않도록 unique 제약조건을 적용한다.
     * </p>
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Refresh Token의 만료 시각이다.
     *
     * <p>
     * JWT 자체의 만료 시간과 DB의 만료 시간을 함께 확인하여
     * 만료된 토큰의 재사용을 방지한다.
     * </p>
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰이 폐기되었는지 나타낸다.
     *
     * <p>
     * 로그아웃하거나 Rotation이 수행되면 true로 변경한다.
     * </p>
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /**
     * 새로운 Refresh Token 저장 객체를 생성한다.
     *
     * <p>
     * Setter나 공개 생성자를 사용하지 않고
     * 의미가 분명한 정적 팩토리 메서드로 생성한다.
     * </p>
     *
     * @param member    토큰을 발급받은 회원
     * @param tokenHash Refresh Token의 SHA-256 해시값
     * @param expiresAt 토큰 만료 시각
     * @return 새 RefreshToken 엔티티
     */
    public static RefreshToken create(
            Member member,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.member = member;
        refreshToken.tokenHash = tokenHash;
        refreshToken.expiresAt = expiresAt;
        refreshToken.revoked = false;

        return refreshToken;
    }

    /**
     * 현재 Refresh Token을 폐기한다.
     *
     * <p>
     * 로그아웃하거나 Refresh Token Rotation이 수행될 때 호출한다.
     * </p>
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * 현재 Refresh Token이 사용 가능한지 확인한다.
     *
     * <p>
     * 폐기되지 않았고 현재 시각보다 만료 시각이 뒤에 있어야 한다.
     * </p>
     *
     * @return 사용할 수 있는 토큰이면 true
     */
    public boolean isUsable() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }
}