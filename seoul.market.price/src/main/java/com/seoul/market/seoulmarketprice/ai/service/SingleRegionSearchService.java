package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.DongMaster;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.DongMasterRepository;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.regex.Matcher;

@Service
public class SingleRegionSearchService {
    private final SggMasterRepository sggRepository;
    private final DongMasterRepository dongRepository;
    private final FastApiService fastApiService;
    private final RestClient aiClient;

    public SingleRegionSearchService(SggMasterRepository sggRepository, DongMasterRepository dongRepository,
                                     FastApiService fastApiService,
                                     @Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.sggRepository = sggRepository;
        this.dongRepository = dongRepository;
        this.fastApiService = fastApiService;
        this.aiClient = aiClient;
    }

    public SingleRegionPriceResponse search(String question) {
        Matcher matcher = RegionQuestionPatterns.FULL_REGION.matcher(question);
        if (!matcher.find()) throw new IllegalArgumentException("구와 동을 입력해주세요. 예: 마포구 서교동 평균 가격 알려줘");
        String guName = matcher.group(1), dongName = matcher.group(2);
        SggMaster gu = sggRepository.findBySggName(guName)
                .orElseThrow(() -> new IllegalArgumentException(guName + "을(를) 찾을 수 없습니다."));
        DongMaster dong = dongRepository.findByDongNameAndSggSggCode(dongName, gu.getSggCode())
                .or(() -> dongRepository.findFirstByDongNameContainingAndSggSggCodeOrderByDongNameAsc(dongName, gu.getSggCode()))
                .orElseThrow(() -> new IllegalArgumentException(guName + " " + dongName + "을(를) 찾을 수 없습니다."));
        ListResponse list = fastApiService.getPyeongList(new ListRequest(gu.getSggCode()));
        ListResponse.ListSummaryDto value = list.groups().values().stream()
                .filter(item -> dong.getDongCode().endsWith(item.code()) || dong.getDongName().equals(item.name()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("해당 지역의 가격 데이터가 없습니다."));
        SingleRegionFacts facts = new SingleRegionFacts(guName + " " + dong.getDongName(),
                value.avg_thing_amt() == null ? 0 : value.avg_thing_amt(),
                value.avg_pyeong_amt() == null ? 0 : value.avg_pyeong_amt(),
                value.total_count() == null ? 0 : value.total_count(), list.baseDate(), "만원", "만원/평");
        SingleRegionPriceRequest request = new SingleRegionPriceRequest("single-search", question, facts,
                List.of(facts.region() + "의 조회 데이터 설명"), List.of("가격 예측", "투자 추천"));
        return aiClient.post().uri("/ai/explain-single-region").body(request).retrieve()
                .body(SingleRegionPriceResponse.class);
    }
}
