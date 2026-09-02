package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces a member's preference-region wording with the saved district name before AI analysis. */
@Component
public class PreferenceRegionResolver {
    public static final String PREFERENCE_REGION_REQUIRED_MESSAGE =
            "선호지역이 설정되어 있지 않습니다. 마이페이지에서 선호 지역을 먼저 설정해주세요.";

    private static final Pattern PREFERENCE_REGION_EXPRESSION = Pattern.compile(
            "내\\s*선호\\s*지역|내가\\s*선호하는\\s*지역|내\\s*관심\\s*지역|내\\s*동네"
    );

    private final MemberRepository memberRepository;
    private final SggMasterRepository sggMasterRepository;

    public PreferenceRegionResolver(MemberRepository memberRepository, SggMasterRepository sggMasterRepository) {
        this.memberRepository = memberRepository;
        this.sggMasterRepository = sggMasterRepository;
    }

    public Resolution resolve(String question, Long memberId) {
        Matcher matcher = PREFERENCE_REGION_EXPRESSION.matcher(question == null ? "" : question);
        if (!matcher.find()) return Resolution.unchanged(question);
        if (memberId == null) return Resolution.preferenceUnavailable();

        Optional<Member> member = memberRepository.findById(memberId);
        String myGu = member.map(Member::getMyGu).filter(value -> !value.isBlank()).orElse(null);
        if (myGu == null) return Resolution.preferenceUnavailable();

        String districtName = sggMasterRepository.findBySggCode(myGu)
                .map(sgg -> sgg.getSggName())
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (districtName == null) return Resolution.preferenceUnavailable();

        String resolvedQuestion = matcher.replaceAll(Matcher.quoteReplacement(districtName));
        return Resolution.resolved(resolvedQuestion);
    }

    public enum Status {
        UNCHANGED,
        RESOLVED,
        PREFERENCE_UNAVAILABLE
    }

    public record Resolution(Status status, String question) {
        static Resolution unchanged(String question) {
            return new Resolution(Status.UNCHANGED, question);
        }

        static Resolution resolved(String question) {
            return new Resolution(Status.RESOLVED, question);
        }

        static Resolution preferenceUnavailable() {
            return new Resolution(Status.PREFERENCE_UNAVAILABLE, null);
        }
    }
}
