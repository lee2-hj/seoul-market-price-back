package com.seoul.market.seoulmarketprice.security.oauth2;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * OAuth2 사용자 정보를 다루는 OAuth2User 구현체.
 *
 * <p>
 * 카카오, 구글 등 OAuth2 로그인 성공 후
 * 사용자 정보를 저장하고 SuccessHandler에 전달한다.
 * </p>
 */
public class CustomOAuth2User implements OAuth2User {

    /**
     * OAuth2 제공자가 전달한 사용자 정보이다.
     */
    private final Map<String, Object> attributes;

    public CustomOAuth2User(
            Map<String, Object> attributes
    ) {
        this.attributes = attributes;
    }

    /**
     * OAuth2 사용자 정보를 반환한다.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * OAuth2 제공자가 발급한 사용자 고유 ID를 반환한다.
     *
     * 카카오 : id
     * 구글 : sub
     */
    @Override
    public String getName() {

        Object id = attributes.get("id");

        if (id != null) {
            return String.valueOf(id);
        }

        return String.valueOf(
                attributes.get("sub")
        );
    }

    /**
     * 서비스 회원 PK를 반환한다.
     */
    public Long getMemberId() {

        return Long.valueOf(
                String.valueOf(
                        attributes.get("memberId")
                )
        );
    }

    /**
     * 서비스 로그인 아이디를 반환한다.
     */
    public String getUserId() {

        return String.valueOf(
                attributes.get("userId")
        );
    }

    /**
     * socialId로 가입된 기존 회원이 존재하는지 여부를 반환한다.
     *
     * true : 기존 회원
     * false : 가입되지 않은 회원
     */
    public boolean memberExists() {

        return Boolean.TRUE.equals(
                attributes.get("memberExists")
        );
    }

    /**
     * 카카오 회원가입(kakao-signup) 흐름으로 들어온 요청인지 여부를
     * 반환한다.
     *
     * true : 회원가입 시도
     * false : 로그인 시도
     */
    public boolean isSignupFlow() {

        return Boolean.TRUE.equals(
                attributes.get("isSignupFlow")
        );
    }

    @Override
    public java.util.Collection<
            ? extends org.springframework.security.core.GrantedAuthority
            > getAuthorities() {

        return java.util.List.of();
    }
}