package com.seoul.market.seoulmarketprice.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 정보를 저장하는 엔티티이다.
 *
 * <p>
 * DB의 {@code tb_admin} 테이블과 매핑된다.
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
@Table(name = "tb_admin")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    /**
     * 관리자 고유번호(PK).
     *
     * <p>
     * tb_admin 테이블의 기본키이며,
     * DB에서 AUTO_INCREMENT 방식으로 자동 생성된다.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 관리자 로그인 아이디.
     *
     * <p>
     * 관리자가 로그인할 때 사용하는 아이디이다.
     * 중복될 수 없다.
     * </p>
     */
    @Column(name = "admin_id", nullable = false, unique = true, length = 30)
    private String adminId;

    /**
     * BCrypt로 암호화된 관리자 비밀번호.
     *
     * <p>
     * 실제 비밀번호가 아닌 BCrypt 암호문을 저장한다.
     * 로그인 시 PasswordEncoder를 이용하여 비교한다.
     * </p>
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * 관리자 이름.
     *
     * <p>
     * 관리자 화면에서 표시되는 이름이다.
     * </p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 로그인에 사용할 비밀번호가 존재하는지 확인한다.
     *
     * @return 비밀번호가 존재하면 true
     */
    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}