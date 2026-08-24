package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.RttRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.RttRespopnse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import com.seoul.market.seoulmarketprice.location.repository.DongMasterRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.regex.Matcher;

@Service
public class TradeTrendSearchService {
    private final SggMasterRepository sggRepository;
    private final DongMasterRepository dongRepository;
    private final FastApiService fastApiService;
    private final RestClient aiClient;

    public TradeTrendSearchService(SggMasterRepository sggRepository, DongMasterRepository dongRepository, FastApiService fastApiService,
                                   @Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.sggRepository = sggRepository;
        this.dongRepository = dongRepository;
        this.fastApiService = fastApiService;
        this.aiClient = aiClient;
    }

    public TradeTrendResponse search(String question) {
        Matcher matcher = RegionQuestionPatterns.FULL_REGION.matcher(question);
        String guName;
        String dongCode = null;
        String region;
        if (matcher.find()) {
            guName = matcher.group(1);
            region = guName + " " + matcher.group(2);
        } else {
            matcher = RegionQuestionPatterns.DISTRICT.matcher(question);
            if (!matcher.find()) throw new IllegalArgumentException("거래 동향을 조회할 자치구 또는 자치동을 입력해 주세요.");
            guName = matcher.group(1);
            region = guName;
        }
        SggMaster sgg = sggRepository.findBySggName(guName)
                .orElseThrow(() -> new IllegalArgumentException(guName + "을 찾지 못했습니다."));
        if (matcher.groupCount() >= 2 && region.contains(" ")) {
            String dongName = region.substring(region.indexOf(' ') + 1);
            dongCode = dongRepository.findByDongNameAndSggSggCode(dongName, sgg.getSggCode())
                    .orElseThrow(() -> new IllegalArgumentException(region + "을 찾지 못했습니다.")).getDongCode();
        }
        RttRespopnse data = fastApiService.getRttInfo(new RttRequest(sgg.getSggCode(), dongCode));
        TradeTrendFacts facts = new TradeTrendFacts(region, data.periodStart(), data.periodEnd(),
                data.totalDealCnt() == null ? 0 : data.totalDealCnt(),
                data.avgTradeAmount() == null ? 0 : data.avgTradeAmount(), "만원", data.volumeChangeRate(),
                data.top5ByVolume() == null ? List.of() : data.top5ByVolume().stream()
                        .map(item -> new TradeTrendFacts.TopApartment(item.aptName(), item.dealCnt() == null ? 0 : item.dealCnt(), item.avgTradeAmount())).toList(),
                data.pyeongDistribution() == null ? List.of() : data.pyeongDistribution().stream()
                        .map(item -> new TradeTrendFacts.PyeongDistribution(item.pyeongGrp(), item.dealCnt() == null ? 0 : item.dealCnt(), item.ratio() == null ? 0 : item.ratio())).toList());
        return aiClient.post().uri("/ai/explain-trade-trend")
                .body(new TradeTrendRequest("trade-trend", question, facts,
                        List.of("조회 기간", "거래 건수", "평균 거래가"), List.of("가격 예측", "투자 추천")))
                .retrieve().body(TradeTrendResponse.class);
    }
}
