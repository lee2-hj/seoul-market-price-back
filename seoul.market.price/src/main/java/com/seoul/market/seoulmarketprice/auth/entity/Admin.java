package com.seoul.market.seoulmarketprice.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 관리자 정보를 저장하는 엔티티이다.
 *
 * <p>
 * DB의 {@code tb_member} 테이블과 매핑된다.
 * </p>
 *
 * <p>
 * 일반 회원(tb_user)과 관리자는 서로 다른 테이블을 사용한다.
 * 따라서 관리자는 회원가입을 하지 않으며,
 * DB에 직접 등록된 계정으로만 로그인할 수 있다.
 * </p>
 *
 * <p>
 * Controller에서는 Entity를 직접 사용하지 않고
 * DTO를 통해 데이터를 주고받는다.
 * </p>
 */
@Entity
@Getter
@Table(name = "tb_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Admin {

    /**
     * 관리자 고유번호(PK).
     *
     * <p>
     * tb_member 테이블의 기본키이며,
     * DB에서 AUTO_INCREMENT 방식으로 자동 생성된다.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "관리자 고유 인덱스")
    private Long id;

    /**
     * 관리자 로그인 아이디.
     *
     * <p>
     * 관리자가 로그인할 때 사용하는 아이디이다.
     * 중복될 수 없다.
     * </p>
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 50, comment = "관리자 로그인 아이디")
    private String adminId;

    /**
     * BCrypt로 암호화된 관리자 비밀번호.
     *
     * <p>
     * 실제 비밀번호가 아닌 BCrypt 암호문을 저장한다.
     * 로그인 시 PasswordEncoder를 이용하여 비교한다.
     * </p>
     */
    @Column(name = "password", nullable = false, comment = "비밀번호")
    private String password;

    /** 현재 로그인 세션의 Refresh Token SHA-256 해시값. */
    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    /**
     * 관리자 이름.
     *
     * <p>
     * 관리자 화면에서 표시되는 이름이다.
     * </p>
     */
    @Column(name = "name", nullable = false, comment = "관리자명")
    private String name;

    /**
     * 로그인에 사용할 비밀번호가 존재하는지 확인한다.
     *
     * @return 비밀번호가 존재하면 true
     */

    /**
     * 관리자 연락처.
     *
     * 관리자 계정의 전화번호를 저장한다.
     */
    @Column(name = "phone", length = 20, comment = "휴대폰 번호")
    private String phone;

    /**
     * 관리자 이메일 주소.
     *
     * 관리자 계정의 이메일을 저장한다.
     */
    @Column(name = "email", length = 100, comment = "이메일")
    private String email;

    @Column(updatable = false)
    private LocalDateTime created_at;

    private LocalDateTime  updated_at;

    private LocalDateTime  deleted_at;

    @PrePersist
    private void prePersist() {
        this.created_at = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * 수정 시 변경 시각을 초 단위까지만 기록한다.
     */
    @PreUpdate
    private void preUpdate() {
        this.updated_at = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    /** 새 로그인 또는 재발급 시 기존 Refresh Token 해시를 덮어쓴다. */
    public void replaceRefreshTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Refresh Token 해시는 비어 있을 수 없습니다.");
        }
        this.refreshTokenHash = tokenHash;
    }

    /** 로그아웃 시 현재 Refresh Token을 무효화한다. */
    public void clearRefreshTokenHash() {
        this.refreshTokenHash = null;
    }
}
