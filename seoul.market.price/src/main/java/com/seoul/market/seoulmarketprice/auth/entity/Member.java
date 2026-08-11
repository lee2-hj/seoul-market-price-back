package com.seoul.market.seoulmarketprice.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

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
            String email,
            Byte isTermsAgreed,
            Byte is_location_agreed,
            Byte is_privacy_agreed,
            String myGu,
            String myDong,
            BigDecimal latitude,
            BigDecimal longitude
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
        member.isTermsAgreed = isTermsAgreed;
        member.isLocationAgreed = is_location_agreed;
        member.isPrivacyAgreed = is_privacy_agreed;
        member.myGu = myGu;
        member.myDong = myDong;
        member.latitude = latitude;
        member.longitude = longitude;

        return member;
    }

    /**
     * OAuth2 소셜 로그인 회원을 생성한다.
     *
     * <p>
     * 카카오, 구글 등 외부 OAuth2 제공자를 통해
     * 로그인한 회원을 생성할 때 사용한다.
     * </p>
     *
     * <p>
     * 소셜 로그인 회원은 서비스 비밀번호를 사용하지 않으므로
     * password는 null로 저장한다.
     * </p>
     *
     * <p>
     * 현재 phone 컬럼은 null을 허용하지만,
     * 기존 카카오 회원 생성 방식과 통일하기 위해
     * 추가 정보 입력 전까지 빈 문자열을 저장한다.
     * </p>
     *
     * @param socialId OAuth2 제공자와 사용자 고유 ID를 조합한 값
     * @param userId 서비스에서 사용할 로그인 아이디
     * @param name 사용자 이름 또는 닉네임
     * @param email 사용자 이메일
     * @return 생성된 소셜 로그인 회원
     */
    public static Member createSocialMember(
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
        member.phone = "";
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
     * id 길이가 google+21자 28개여서 length 20->50으로 늘림
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 50, comment = "로그인에 사용하는 유저 아이디")
    private String userId;

    /**
     * 암호화된 비밀번호.
     *
     * 일반 로그인 사용자는 BCrypt 암호화 값을 저장한다.
     * 소셜 로그인 사용자는 null일 수 있다.
     */
    @Column(name = "password", nullable = true, comment = "비밀번호")
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
    @Column(name = "phone", nullable = true, comment = "휴대전화 번호")
    private String phone;

    /** PASS 본인인증 연계정보. 기존 회원의 점진적 전환을 위해 null을 허용한다. */
    @Column(name = "ci", unique = true, length = 255, comment = "PASS 본인인증 CI")
    private String ci;

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

    @Column(name = "is_terms_agreed", nullable = true, columnDefinition = "TINYINT(1)", comment = "이용약관 동의 여부 0: 미동의, 1: 동의")
    private Byte isTermsAgreed;

    @Column(name = "is_location_agreed", nullable = true, columnDefinition = "TINYINT(1)", comment = "위치기반 서비스 이용약관 동의 여부 0: 미동의, 1: 동의")
    private Byte isLocationAgreed;

    @Column(name = "is_privacy_agreed", nullable = true, columnDefinition = "TINYINT(1)", comment = "개인정보 수집 및 이용동의 0: 미동의, 1: 동의")
    private Byte isPrivacyAgreed;

    /** 사용자가 가격 정보를 우선 확인하려는 서울시 자치구. */
    @Column(name = "my_gu", length = 50, comment = "유저 선호 자치구")
    private String myGu;

    /** 사용자가 선택한 자치구 안의 선호 행정동. */
    @Column(name = "my_dong", length = 50, comment = "유저 선호 행정동")
    private String myDong;

    /** 사용자 선호 위치의 위도이며 소수점 이하 7자리까지 저장한다. */
    @Column(name = "latitude", precision = 10, scale = 7, comment = "유저 선호 위치 위도")
    private BigDecimal latitude;

    /** 사용자 선호 위치의 경도이며 소수점 이하 7자리까지 저장한다. */
    @Column(name = "longitude", precision = 10, scale = 7, comment = "유저 선호 위치 경도")
    private BigDecimal longitude;

    @Column(updatable = false)
    private LocalDateTime created_at;

    private LocalDateTime  updated_at;

    private LocalDateTime  deleted_at;

    /**
     * 저장 전 생성 시각을 초 단위까지만 기록한다.
     *
     * 생성 시점에는 updated_at을 채우지 않는다.
     */
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

    public boolean hasCi() {
        return ci != null && !ci.isBlank();
    }

    /** 회원이 소프트 삭제된 상태인지 확인한다. */
    public boolean isDeleted() {
        return deleted_at != null;
    }

    /** 회원을 소프트 삭제 상태로 전환한다. */
    public void withdraw() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.deleted_at = now;
        this.updated_at = now;
    }

    /** 최초 확인된 CI만 등록하며, 이미 등록된 CI는 다른 값으로 변경할 수 없다. */
    public void registerCi(String verifiedCi) {
        if (verifiedCi == null || verifiedCi.isBlank()) {
            throw new IllegalArgumentException("본인인증 CI를 확인할 수 없습니다.");
        }
        if (hasCi() && !ci.equals(verifiedCi)) {
            throw new IllegalArgumentException("기존 본인인증 정보와 일치하지 않습니다.");
        }
        this.ci = verifiedCi;
    }

    /** 일반 로그인 회원의 비밀번호를 새 BCrypt 값으로 교체한다. */
    public void changePassword(String encodedPassword) {
        if (!isLocalUser()) {
            throw new IllegalStateException(
                    "일반 로그인 회원만 비밀번호를 변경할 수 있습니다."
            );
        }
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "암호화된 비밀번호는 비어 있을 수 없습니다."
            );
        }
        this.password = encodedPassword;
    }
}
