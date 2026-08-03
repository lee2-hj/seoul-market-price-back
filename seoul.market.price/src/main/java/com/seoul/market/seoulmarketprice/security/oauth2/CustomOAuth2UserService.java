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
 * 일반 회원가입/로그인이 분리되어 있는 것과 동일하게,
 * 소셜 로그인도 제공자별로 로그인용 client 등록(kakao, google)과
 * 회원가입용 client 등록(kakao-signup, google-signup)을 분리해서
 * 사용한다.
 * </p>
 *
 * <p>
 * 로그인용과 회원가입용은 client-id/secret과 실제 계정 체계
 * (socialId)는 동일하게 공유하고, redirect-uri만 다르다.
 * registrationId가 "-signup"으로 끝나면 회원가입 시도로 판단하고,
 * 나머지는 로그인 시도로 판단한다.
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
     * 회원가입용 registration 이름에 붙는 접미사이다.
     *
     * <p>
     * 예: kakao-signup, google-signup
     * </p>
     */
    private static final String SIGNUP_SUFFIX = "-signup";

    /**
     * OAuth2 제공자로부터 사용자 정보를 조회하고,
     * socialId로 가입된 기존 회원인지 확인한다.
     *
     * <p>
     * 로그인(kakao, google)은 회원가입을 대신하지 않으므로
     * 기존 회원이 아니면 새 회원을 만들지 않는다.
     * </p>
     *
     * <p>
     * 반대로 회원가입(kakao-signup, google-signup)은 기존 회원이
     * 아닐 때만 새 회원을 생성한다. 이미 가입된 계정이면 중복
     * 가입하지 않고 OAuth2SuccessHandler에서 안내하도록 넘긴다.
     * </p>
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
         * "-signup"으로 끝나는 registrationId는 회원가입 시도이다.
         *
         * 로그인용(kakao, google)과 회원가입용(kakao-signup,
         * google-signup)은 실제로는 같은 제공자 계정 체계를
         * 사용하므로, socialId 계산과 속성 파싱에는 접미사를 뗀
         * provider 이름을 그대로 쓴다.
         */
        boolean isSignupFlow =
                registrationId.endsWith(SIGNUP_SUFFIX);

        String provider =
                isSignupFlow
                        ? registrationId.substring(
                                0,
                                registrationId.length()
                                        - SIGNUP_SUFFIX.length()
                        )
                        : registrationId;

        /*
         * 제공자별 원본 사용자 고유 ID이다.
         *
         * 카카오는 id,
         * 구글은 sub 속성을 사용한다.
         */
        String providerUserId;

        /*
         * 회원가입에서 신규 회원을 생성할 때만 필요하다.
         */
        String nickname = null;
        String email = null;

        switch (provider) {

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

                if (isSignupFlow) {
                    nickname = extractKakaoNickname(attributes);
                    email = extractKakaoEmail(attributes);
                }
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

                if (isSignupFlow) {
                    nickname =
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
         *
         * 회원가입용 registration으로 처리해도 provider가
         * 로그인용과 동일하게 정규화되어 있으므로, 로그인(kakao,
         * google)에서 계산하는 socialId와 동일한 값이 나온다.
         */
        String socialId =
                provider
                        + "_"
                        + providerUserId;

        /*
         * socialId로 기존 회원을 조회한다.
         */
        Optional<Member> existingMember =
                memberRepository.findBySocialId(
                        socialId
                );

        boolean memberExists =
                existingMember.isPresent();

        /*
         * 회원 매칭 여부를 기록한다.
         *
         * "기존 회원인데 못 찾는다"는 문제는 대부분
         * DB에 저장된 social_id와 이번 로그인에서 계산한 socialId가
         * 실제로 다른 값이기 때문에 발생한다.
         * (예: 카카오 앱(REST API 키)이 바뀌면 같은 사람이어도
         * 카카오가 내려주는 회원번호 자체가 달라진다.)
         *
         * 두 값을 함께 로그로 남겨 즉시 비교할 수 있게 한다.
         */
        log.info(
                "{} 회원 매칭 결과: socialId={}, memberExists={}",
                registrationId,
                socialId,
                memberExists
        );

        /*
         * OAuth2SuccessHandler에서 사용할
         * 서비스 회원 정보를 attributes에 추가한다.
         */
        attributes.put(
                "memberExists",
                memberExists
        );

        attributes.put(
                "isSignupFlow",
                isSignupFlow
        );

        attributes.put(
                "registrationId",
                registrationId
        );

        /*
         * 로그인(kakao, google): 기존 회원만 사용하고,
         * 없으면 새로 만들지 않는다.
         *
         * 회원가입(kakao-signup): 기존 회원이 없을 때만
         * 새로 만든다. 이미 가입된 회원이면 중복 가입시키지 않고
         * member를 null로 남겨 OAuth2SuccessHandler가
         * "이미 가입된 회원입니다" 안내를 하도록 한다.
         */
        Member member = null;

        if (isSignupFlow) {

            if (!memberExists) {

                member =
                        memberRepository.save(
                                Member.createSocialMember(
                                        socialId,
                                        socialId,
                                        nickname,
                                        email
                                )
                        );

                log.info(
                        "{} 회원가입 완료: memberId={}, socialId={}",
                        provider,
                        member.getId(),
                        socialId
                );
            }

        } else if (memberExists) {

            member = existingMember.get();
        }

        if (member != null) {

            attributes.put(
                    "memberId",
                    member.getId()
            );

            attributes.put(
                    "userId",
                    member.getUserId()
            );
        }

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
     * <p>
     * 카카오 회원가입으로 신규 회원을 생성할 때만 사용한다.
     * </p>
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
     * <p>
     * 카카오 회원가입으로 신규 회원을 생성할 때만 사용한다.
     * </p>
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
     * <p>
     * 회원가입으로 신규 회원을 생성할 때만 사용한다.
     * </p>
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