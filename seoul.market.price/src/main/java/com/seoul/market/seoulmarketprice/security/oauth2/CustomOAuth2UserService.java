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

/**
 * 카카오 사용자 정보를 조회하고 회원을 처리하는 서비스.
 */
@Service
@Transactional(readOnly = true)
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final MemberRepository memberRepository;

    public CustomOAuth2UserService(
            MemberRepository memberRepository
    ) {
        this.memberRepository = memberRepository;
    }

    /**
     * 카카오 사용자 정보를 불러온다.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {

        // 카카오 사용자 정보 요청
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attributes =
                new HashMap<>(oauth2User.getAttributes());

        // 카카오가 응답한 원본 속성을 확인하기 위한 로그
        log.info("카카오 사용자 정보 응답: {}", attributes);

        // 카카오 회원 고유 ID
        String socialId = String.valueOf(
                attributes.get("id")
        );

        String nickname = extractNickname(attributes);
        String email = extractEmail(attributes);

        // 서비스에서 사용할 로그인 아이디
        String userId = "kakao_" + socialId;

        // 기존 회원 조회 또는 신규 소셜 회원 저장
        Member member = memberRepository
                .findBySocialId(socialId)
                .orElseGet(() ->
                        memberRepository.save(
                                Member.createKakaoMember(
                                        socialId,
                                        userId,
                                        nickname,
                                        email
                                )
                        )
                );

        // 성공 핸들러에서 사용할 회원 정보 추가
        attributes.put("memberId", member.getId());
        attributes.put("userId", member.getUserId());

        return new KakaoOAuth2User(attributes);
    }

    /**
     * 카카오 닉네임을 조회한다.
     */
    @SuppressWarnings("unchecked")
    private String extractNickname(
            Map<String, Object> attributes
    ) {
        Map<String, Object> account =
                (Map<String, Object>) attributes.get("kakao_account");

        if (account != null) {
            Map<String, Object> profile =
                    (Map<String, Object>) account.get("profile");

            if (profile != null && profile.get("nickname") != null) {
                return String.valueOf(profile.get("nickname"));
            }
        }

        // 닉네임이 없을 때 기본값 사용
        return "카카오사용자";
    }

    /**
     * 카카오 이메일을 조회한다.
     */
    @SuppressWarnings("unchecked")
    private String extractEmail(
            Map<String, Object> attributes
    ) {
        Map<String, Object> account =
                (Map<String, Object>) attributes.get("kakao_account");

        if (account == null || account.get("email") == null) {
            return null;
        }

        return String.valueOf(account.get("email"));
    }
}