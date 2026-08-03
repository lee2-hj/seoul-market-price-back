package com.seoul.market.seoulmarketprice.security.oauth2;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 사용자 정보를 조회하고 회원을 처리하는 서비스이다.
 *
 * <p>
 * 현재 카카오와 구글 로그인을 지원한다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    /**
     * OAuth2 사용자 정보와 처리 결과를 기록하는 로그 객체이다.
     */
    private static final Logger log =
            LoggerFactory.getLogger(
                    CustomOAuth2UserService.class
            );

    /**
     * 소셜 로그인 회원을 조회하고 저장하는 Repository이다.
     */
    private final MemberRepository memberRepository;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param memberRepository 회원 Repository
     */
    public CustomOAuth2UserService(
            MemberRepository memberRepository
    ) {
        this.memberRepository = memberRepository;
    }

    /**
     * OAuth2 제공자로부터 사용자 정보를 조회하고,
     * 기존 회원 로그인 또는 신규 회원가입을 처리한다.
     *
     * @param userRequest OAuth2 사용자 정보 요청
     * @return 카카오와 구글에서 공통으로 사용하는 OAuth2 사용자 객체
     * @throws OAuth2AuthenticationException OAuth2 인증 처리 실패
     */
    @Override
    @Transactional
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {

        /*
         * OAuth2 제공자의 사용자 정보 API를 호출한다.
         */
        OAuth2User oauth2User =
                super.loadUser(userRequest);

        /*
         * 이후 서비스 회원 정보를 추가할 수 있도록
         * 수정 가능한 HashMap으로 복사한다.
         */
        Map<String, Object> attributes =
                new HashMap<>(
                        oauth2User.getAttributes()
                );

        /*
         * application.yml의 OAuth2 registration 이름을 조회한다.
         *
         * 카카오 로그인은 kakao,
         * 구글 로그인은 google이 반환된다.
         */
        String registrationId =
                userRequest
                        .getClientRegistration()
                        .getRegistrationId();

        /*
         * OAuth2 제공자가 응답한 원본 사용자 정보를 기록한다.
         *
         * 운영 환경에서는 개인정보가 로그에 남지 않도록
         * 로그 수준이나 출력 항목을 조정할 필요가 있다.
         */
        log.info(
                "{} 사용자 정보 응답: {}",
                registrationId,
                attributes
        );

        /*
         * 제공자별 원본 사용자 고유 ID이다.
         *
         * 카카오는 id,
         * 구글은 sub 속성을 사용한다.
         */
        String providerUserId;

        /*
         * 사용자 이름 또는 닉네임이다.
         */
        String name;

        /*
         * 사용자 이메일이다.
         */
        String email;

        /*
         * OAuth2 제공자에 따라 사용자 정보 구조가 다르므로
         * 제공자별로 필요한 값을 분리하여 추출한다.
         */
        switch (registrationId) {

            /*
             * 카카오 사용자 정보 처리
             */
            case "kakao" -> {
                providerUserId =
                        getRequiredAttribute(
                                attributes,
                                "id",
                                "카카오 회원 고유 ID"
                        );

                name =
                        extractKakaoNickname(
                                attributes
                        );

                email =
                        extractKakaoEmail(
                                attributes
                        );
            }

            /*
             * 구글 사용자 정보 처리
             */
            case "google" -> {
                providerUserId =
                        getRequiredAttribute(
                                attributes,
                                "sub",
                                "구글 회원 고유 ID"
                        );

                name =
                        getOptionalAttribute(
                                attributes,
                                "name",
                                "구글사용자"
                        );

                email =
                        getOptionalAttribute(
                                attributes,
                                "email",
                                null
                        );
            }

            /*
             * 프로젝트에서 지원하지 않는 OAuth2 제공자는
             * 인증을 중단한다.
             */
            default -> throw new OAuth2AuthenticationException(
                    "지원하지 않는 OAuth2 제공자입니다: "
                            + registrationId
            );
        }

        /*
         * 서로 다른 OAuth2 제공자의 사용자 ID가 우연히 같더라도
         * 충돌하지 않도록 제공자 이름과 사용자 ID를 조합한다.
         *
         * 예:
         * kakao_123456789
         * google_123456789
         */
        String socialId =
                registrationId
                        + "_"
                        + providerUserId;

        /*
         * 서비스에서 사용할 로그인 아이디이다.
         *
         * 현재는 socialId와 같은 값을 사용한다.
         */
        String userId = socialId;

        /*
         * socialId로 기존 회원을 조회한다.
         */
        Optional<Member> existingMember =
                memberRepository.findBySocialId(
                        socialId
                );

        /*
         * 기존 회원이 없으면 신규 소셜 회원가입으로 판단한다.
         */
        boolean isNewMember =
                existingMember.isEmpty();

        /*
         * 기존 회원은 그대로 사용하고,
         * 신규 회원은 tb_user에 저장한다.
         */
        Member member =
                existingMember.orElseGet(() ->
                        memberRepository.save(
                                Member.createSocialMember(
                                        socialId,
                                        userId,
                                        name,
                                        email
                                )
                        )
                );

        /*
         * OAuth2SuccessHandler에서 사용할
         * 서비스 회원 정보를 attributes에 추가한다.
         */
        attributes.put(
                "memberId",
                member.getId()
        );

        attributes.put(
                "userId",
                member.getUserId()
        );

        attributes.put(
                "isNewMember",
                isNewMember
        );

        attributes.put(
                "registrationId",
                registrationId
        );

        /*
         * 카카오와 구글에서 공통으로 사용하는
         * OAuth2 사용자 객체를 반환한다.
         */
        return new CustomOAuth2User(
                attributes
        );
    }

    /**
     * 카카오 사용자 정보에서 닉네임을 조회한다.
     *
     * @param attributes 카카오 사용자 정보
     * @return 카카오 닉네임
     */
    @SuppressWarnings("unchecked")
    private String extractKakaoNickname(
            Map<String, Object> attributes
    ) {
        Map<String, Object> account =
                (Map<String, Object>)
                        attributes.get(
                                "kakao_account"
                        );

        if (account != null) {
            Map<String, Object> profile =
                    (Map<String, Object>)
                            account.get(
                                    "profile"
                            );

            if (
                    profile != null
                            && profile.get("nickname") != null
            ) {
                return String.valueOf(
                        profile.get("nickname")
                );
            }
        }

        /*
         * 닉네임이 없을 때 사용할 기본값이다.
         */
        return "카카오사용자";
    }

    /**
     * 카카오 사용자 정보에서 이메일을 조회한다.
     *
     * @param attributes 카카오 사용자 정보
     * @return 카카오 이메일 또는 null
     */
    @SuppressWarnings("unchecked")
    private String extractKakaoEmail(
            Map<String, Object> attributes
    ) {
        Map<String, Object> account =
                (Map<String, Object>)
                        attributes.get(
                                "kakao_account"
                        );

        if (
                account == null
                        || account.get("email") == null
        ) {
            return null;
        }

        return String.valueOf(
                account.get("email")
        );
    }

    /**
     * OAuth2 사용자 정보에서 필수 속성을 조회한다.
     *
     * <p>
     * 필수 속성이 없으면 회원을 식별할 수 없으므로
     * OAuth2 인증을 중단한다.
     * </p>
     *
     * @param attributes OAuth2 사용자 정보
     * @param key 조회할 속성 이름
     * @param description 오류 메시지에 표시할 설명
     * @return 문자열로 변환한 속성값
     */
    private String getRequiredAttribute(
            Map<String, Object> attributes,
            String key,
            String description
    ) {
        Object value =
                attributes.get(key);

        if (
                value == null
                        || String.valueOf(value).isBlank()
        ) {
            throw new OAuth2AuthenticationException(
                    description
                            + "를 조회할 수 없습니다."
            );
        }

        return String.valueOf(value);
    }

    /**
     * OAuth2 사용자 정보에서 선택 속성을 조회한다.
     *
     * @param attributes OAuth2 사용자 정보
     * @param key 조회할 속성 이름
     * @param defaultValue 속성이 없을 때 사용할 기본값
     * @return 속성값 또는 기본값
     */
    private String getOptionalAttribute(
            Map<String, Object> attributes,
            String key,
            String defaultValue
    ) {
        Object value =
                attributes.get(key);

        if (
                value == null
                        || String.valueOf(value).isBlank()
        ) {
            return defaultValue;
        }

        return String.valueOf(value);
    }
}