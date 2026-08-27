package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 순위 질의의 자치구/자치동 범위를 일관되게 해석한다. */
final class RankingRegionResolver {
    private static final Pattern FULL_REGION = Pattern.compile("([가-힣]+구)\\s*([가-힣]+(?:동|가))");
    private static final Pattern DISTRICT = Pattern.compile("([가-힣]+구)");
    private static final Pattern KOREAN_TOKEN = Pattern.compile("[가-힣]{2,}");
    private static final Pattern LOCATION_TOKEN = Pattern.compile("([가-힣]{2,}?)(?:에서|의|을|를|에)");

    private final SggMasterRepository sggRepository;
    private final LocationMasterService locationService;

    RankingRegionResolver(SggMasterRepository sggRepository, LocationMasterService locationService) {
        this.sggRepository = sggRepository;
        this.locationService = locationService;
    }

    ResolvedRegion resolve(String question) {
        if (isExplicitAllSeoul(question)) return ResolvedRegion.allSeoulScope();

        Matcher fullRegion = FULL_REGION.matcher(question);
        if (fullRegion.find()) {
            SggMaster sgg = findExact(fullRegion.group(1));
            DongRegionResponse dong = locationService.resolveDong(fullRegion.group(2)).stream()
                    .filter(candidate -> candidate.sggCode().equals(sgg.getSggCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("자치구와 자치동 조합을 찾을 수 없습니다."));
            return new ResolvedRegion(sgg.getSggCode(), dong.dongCode(),
                    sgg.getSggName() + " " + dong.dongName(), false);
        }

        Matcher district = DISTRICT.matcher(question);
        if (district.find()) {
            SggMaster sgg = findExact(district.group(1));
            return new ResolvedRegion(sgg.getSggCode(), null, sgg.getSggName(), false);
        }

        List<SggMaster> districts = sggRepository.findAllByOrderBySggNameAsc();
        List<SggMaster> aliases = districts.stream().filter(sgg -> containsAlias(question, sgg.getSggName())).toList();
        if (aliases.size() == 1) {
            SggMaster sgg = aliases.getFirst();
            return new ResolvedRegion(sgg.getSggCode(), null, sgg.getSggName(), false);
        }
        if (aliases.size() > 1) {
            throw new IllegalArgumentException("입력한 지역과 일치하는 자치구가 여러 개입니다. 자치구 이름을 정확히 입력해주세요.");
        }

        String suggestion = findSuggestion(question, districts);
        if (suggestion != null) throw new IllegalArgumentException(suggestion + "를 의미하셨나요?");
        throw new IllegalArgumentException("입력한 지역을 찾을 수 없습니다. 자치구 또는 자치동 이름을 입력해주세요.");
    }

    private SggMaster findExact(String name) {
        return sggRepository.findBySggName(name)
                .orElseThrow(() -> new IllegalArgumentException(name + "을(를) 찾을 수 없습니다."));
    }

    private boolean isExplicitAllSeoul(String question) {
        return question.replaceAll("\\s+", "").contains("서울전체");
    }

    private boolean containsAlias(String question, String districtName) {
        String alias = districtName.endsWith("구") ? districtName.substring(0, districtName.length() - 1) : districtName;
        return question.contains(districtName) || question.contains(alias);
    }

    private String findSuggestion(String question, List<SggMaster> districts) {
        List<String> tokens = LOCATION_TOKEN.matcher(question).results()
                .map(match -> match.group(1)).toList();
        if (tokens.isEmpty()) tokens = KOREAN_TOKEN.matcher(question).results().map(match -> match.group()).toList();
        return tokens.stream()
                .flatMap(token -> districts.stream()
                        .map(SggMaster::getSggName)
                        .filter(name -> levenshtein(token, alias(name)) <= 1))
                .min(Comparator.comparingInt(String::length))
                .orElse(null);
    }

    private String alias(String districtName) {
        return districtName.endsWith("구") ? districtName.substring(0, districtName.length() - 1) : districtName;
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[right.length()];
    }

    record ResolvedRegion(String sggCode, String dongCode, String name, boolean allSeoul) {
        static ResolvedRegion allSeoulScope() {
            return new ResolvedRegion(null, null, "서울 전체", true);
        }
    }
}
