package com.seoul.market.seoulmarketprice.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티.
 *
 * 팀 공통 테이블 정의서의 tb_user 테이블과 매핑된다.
 *
 * Controller의 요청과 응답에는 직접 사용하지 않고,
 * 외부 데이터 전달에는 record DTO를 사용한다.
 *
 * Setter는 제공하지 않는다.
 */
@Entity
@Getter
@Table(name = "tb_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    /**
     * 아이디와 비밀번호로 로그인하는 일반 회원을 생성한다.
     */
    public static Member createLocalMember(
            String userId,
            String encodedPassword,
            String name,
            String zipcode,
            String address,
            String addressDetail,
            String phone,
            String email
    ) {
        Member member = new Member();

        member.socialId = null;
        member.userId = userId;
        member.password = encodedPassword;
        member.name = name;
        member.zipcode = zipcode;
        member.address = address;
        member.addressDetail = addressDetail;
        member.phone = phone;
        member.email = email;
        member.userType = UserType.LOCAL;

        return member;
    }

    /**
     * 카카오 로그인 회원을 생성한다.
     */
    public static Member createKakaoMember(
            String socialId,
            String userId,
            String name,
            String email
    ) {
        Member member = new Member();

        member.socialId = socialId;
        member.userId = userId;
        member.password = null;
        member.name = name;
        member.email = email;
        member.phone = null;
        member.userType = UserType.SOCIAL;

        return member;
    }

    /**
     * 회원 고유 인덱스.
     *
     * DB에서 AUTO_INCREMENT 방식으로 생성된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "유저 고유 인덱스")
    private Long id;

    /**
     * 로그인에 사용하는 사용자 아이디.
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 20, comment = "로그인에 사용하는 유저 아이디")
    private String userId;

    /**
     * 암호화된 비밀번호.
     *
     * 일반 로그인 사용자는 BCrypt 암호화 값을 저장한다.
     * 소셜 로그인 사용자는 null일 수 있다.
     */
    @Column(name = "password", comment = "비밀번호")
    private String password;

    /**
     * 사용자 실명 또는 닉네임.
     */
    @Column(name = "name", nullable = false, comment = "유저명")
    private String name;

    /**
     * 배송지 우편번호.
     */
    @Column(name = "zipcode", comment = "우편번호")
    private String zipcode;

    /**
     * 기본 주소.
     */
    @Column(name = "address", comment = "주소")
    private String address;

    /**
     * 상세 주소.
     */
    @Column(name = "address_detail", comment = "상세주소")
    private String addressDetail;

    /**
     * 휴대전화 번호.
     *
     * 일반 회원은 필수이며,
     * 소셜 회원은 추가 정보 입력 전까지 null일 수 있다.
     */
    @Column(name = "phone", comment = "휴대전화 번호")
    private String phone;

    /**
     * 이메일 주소.
     */
    @Column(name = "email", comment = "이메일")
    private String email;

    /**
     * 사용자 로그인 유형.
     *
     * DB에는 TINYINT 값으로 저장된다.
     *
     * 0: 일반 사용자
     * 1: 소셜 로그인 사용자
     */
    /**
     * 소셜 로그인 제공자가 발급한 사용자 고유 ID.
     *
     * 일반 로그인 회원은 null일 수 있다.
     */
    @Column(name = "social_id", unique = true, length = 100, comment = "소셜 로그인 제공자가 발급한 유저 고유 ID")
    private String socialId;


    @Convert(converter = UserTypeConverter.class)
    @Column(name = "user_type", nullable = false, comment = "유저 가입 유형 0: 일반 사용자, 1: 소셜 로그인 사용자")
    private UserType userType;

    /**
     * 일반 로그인 사용자인지 확인한다.
     *
     * @return 일반 로그인 사용자이면 true
     */
    public boolean isLocalUser() {
        return userType == UserType.LOCAL;
    }

    /**
     * 로그인에 사용할 비밀번호가 존재하는지 확인한다.
     *
     * 소셜 로그인 사용자는 비밀번호가 null일 수 있다.
     *
     * @return 비밀번호가 존재하면 true
     */


    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}
