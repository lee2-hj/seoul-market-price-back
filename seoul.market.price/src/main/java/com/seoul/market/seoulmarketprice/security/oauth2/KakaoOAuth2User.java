package com.seoul.market.seoulmarketprice.security.oauth2;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * 카카오 사용자 정보를 다루는 OAuth2User 구현체.
 */
public class KakaoOAuth2User implements OAuth2User {

    private final Map<String, Object> attributes;

    public KakaoOAuth2User(
            Map<String, Object> attributes
    ) {
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(
                attributes.get("id")
        );
    }

    /**
     * 카카오 회원 고유 ID.
     */
    public Long getKakaoId() {

        return Long.valueOf(
                String.valueOf(attributes.get("id"))
        );
    }

    /**
     * 닉네임 조회.
     */
    @SuppressWarnings("unchecked")
    public String getNickname() {

        Map<String, Object> properties =
                (Map<String, Object>) attributes.get("properties");

        return (String) properties.get("nickname");
    }

    /**
     * 이메일 조회.
     */
    @SuppressWarnings("unchecked")
    public String getEmail() {

        Map<String, Object> account =
                (Map<String, Object>) attributes.get("kakao_account");

        return (String) account.get("email");
    }

    /**
     * 서비스 회원 PK를 반환한다.
     */
    public Long getMemberId() {
        return Long.valueOf(
                String.valueOf(attributes.get("memberId"))
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
     * 이번 인증으로 새로 가입된 회원인지 여부를 반환한다.
     *
     * true면 카카오 회원가입, false면 카카오 로그인으로 처리한다.
     */
    public boolean isNewMember() {
        return Boolean.TRUE.equals(
                attributes.get("isNewMember")
        );
    }

    @Override
    public java.util.Collection<
            ? extends org.springframework.security.core.GrantedAuthority
            > getAuthorities() {

        return java.util.List.of();
    }
}