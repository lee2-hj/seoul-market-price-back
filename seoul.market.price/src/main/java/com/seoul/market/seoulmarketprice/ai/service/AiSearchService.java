package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.CompareRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.CompareResponse;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.DongMaster;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.DongMasterRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AiSearchService {
    private static final Pattern REGION_PATTERN = Pattern.compile("([가-힣]+구)\\s+([가-힣]+동)");
    private static final Pattern DISTRICT_PATTERN = Pattern.compile("([가-힣]+구)");
    private final SggMasterRepository sggRepository;
    private final DongMasterRepository dongRepository;
    private final FastApiService fastApiService;
    private final AiExplanationService explanationService;

    public AiSearchService(SggMasterRepository sggRepository, DongMasterRepository dongRepository,
                           FastApiService fastApiService, AiExplanationService explanationService) {
        this.sggRepository = sggRepository;
        this.dongRepository = dongRepository;
        this.fastApiService = fastApiService;
        this.explanationService = explanationService;
    }

    public PriceComparisonResponse search(String question) {
        Matcher matcher = REGION_PATTERN.matcher(question);
        String[] names = new String[4];
        int i = 0;
        while (matcher.find() && i < 4) { names[i++] = matcher.group(1); names[i++] = matcher.group(2); }
        if (i == 0) return searchDistricts(question);
        if (i != 4) throw new IllegalArgumentException("비교할 지역을 두 곳 모두 입력해주세요. 예: 강동구와 강서구 또는 마포구 서교동과 성동구 성수동");
        Region a = findRegion(names[0], names[1]);
        Region b = findRegion(names[2], names[3]);
        CompareResponse compare = fastApiService.getCompare(new CompareRequest(a.guCode, a.dongCode, b.guCode, b.dongCode));
        long aAvg = compare.region1().avgThingAmt() == null ? 0 : compare.region1().avgThingAmt();
        long bAvg = compare.region2().avgThingAmt() == null ? 0 : compare.region2().avgThingAmt();
        String higher = aAvg >= bAvg ? a.label : b.label;
        String lower = aAvg >= bAvg ? b.label : a.label;
        PriceFacts facts = new PriceFacts(a.label, b.label, aAvg, bAvg, "원", higher, lower, Math.abs(aAvg - bAvg));
        return explanationService.explain(new PriceComparisonRequest("main-search", question, facts,
                java.util.List.of(higher + " 가격이 높음"), java.util.List.of(lower + " 가격이 높음")));
    }

    private PriceComparisonResponse searchDistricts(String question) {
        Matcher matcher = DISTRICT_PATTERN.matcher(question);
        String[] names = new String[2];
        int count = 0;
        while (matcher.find() && count < names.length) names[count++] = matcher.group(1);
        if (count != 2) {
            throw new IllegalArgumentException("비교할 자치구를 두 곳 모두 입력해주세요. 예: 강동구와 강서구 비교해줘");
        }

        District a = findDistrict(names[0]);
        District b = findDistrict(names[1]);
        long aAvg = districtAverage(a.code());
        long bAvg = districtAverage(b.code());
        String higher = aAvg >= bAvg ? a.name() : b.name();
        String lower = aAvg >= bAvg ? b.name() : a.name();
        PriceFacts facts = new PriceFacts(a.name(), b.name(), aAvg, bAvg, "원", higher, lower,
                Math.abs(aAvg - bAvg));
        return explanationService.explain(new PriceComparisonRequest("district-comparison-search", question, facts,
                java.util.List.of(higher + " 평균 가격이 높음"), java.util.List.of("조회된 실거래 데이터 기준")));
    }

    private District findDistrict(String name) {
        SggMaster district = sggRepository.findBySggName(name)
                .orElseThrow(() -> new IllegalArgumentException(name + "을(를) 찾을 수 없습니다."));
        return new District(district.getSggCode(), name);
    }

    private long districtAverage(String districtCode) {
        ListResponse list = fastApiService.getPyeongList(new ListRequest(districtCode));
        long weightedSum = 0;
        long countSum = 0;
        for (ListResponse.ListSummaryDto item : list.groups().values()) {
            long count = item.total_count() == null ? 0 : item.total_count();
            if (item.avg_thing_amt() != null && count > 0) {
                weightedSum += item.avg_thing_amt() * count;
                countSum += count;
            }
        }
        if (countSum == 0) throw new IllegalArgumentException("해당 자치구의 가격 데이터가 없습니다.");
        return Math.round((double) weightedSum / countSum);
    }

    private Region findRegion(String guName, String dongName) {
        SggMaster gu = sggRepository.findBySggName(guName).orElseThrow(() -> new IllegalArgumentException(guName + "을(를) 찾을 수 없습니다."));
        DongMaster dong = dongRepository.findByDongNameAndSggSggCode(dongName, gu.getSggCode())
                .or(() -> dongRepository.findFirstByDongNameContainingAndSggSggCodeOrderByDongNameAsc(
                        dongName.replaceAll("[0-9]", ""), gu.getSggCode()))
                .orElseThrow(() -> new IllegalArgumentException(guName + " " + dongName + "을(를) 찾을 수 없습니다."));
        return new Region(gu.getSggCode(), dong.getDongCode(), guName + " " + dongName);
    }
    private record Region(String guCode, String dongCode, String label) {}
    private record District(String code, String name) {}
}
