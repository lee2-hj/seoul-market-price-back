package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.*;
import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TopBottomSearchService {
    private static final Pattern GU_PATTERN = Pattern.compile("([가-힣]+구)");
    private final SggMasterRepository sggRepository;
    private final FastApiService fastApiService;
    private final RestClient aiClient;

    public TopBottomSearchService(SggMasterRepository sggRepository, FastApiService fastApiService,
                                  @Qualifier("aiFastApiRestClient") RestClient aiClient) {
        this.sggRepository = sggRepository;
        this.fastApiService = fastApiService;
        this.aiClient = aiClient;
    }

    public SingleRegionPriceResponse search(String question) {
        Matcher matcher = GU_PATTERN.matcher(question);
        if (!matcher.find()) throw new IllegalArgumentException("서울시 자치구를 입력해주세요.");
        String guName = matcher.group(1);
        SggMaster gu = sggRepository.findBySggName(guName)
                .orElseThrow(() -> new IllegalArgumentException(guName + "을(를) 찾을 수 없습니다."));
        ListResponse list = fastApiService.getPyeongList(new ListRequest(gu.getSggCode()));
        boolean lowest = question.contains("낮") || question.contains("최저") || question.contains("저렴");
        Comparator<ListResponse.ListSummaryDto> comparator = Comparator.comparingLong(
                item -> item.avg_pyeong_amt() == null ? 0 : item.avg_pyeong_amt());
        ListResponse.ListSummaryDto selected = list.groups().values().stream()
                .filter(item -> item.avg_pyeong_amt() != null)
                .min(lowest ? comparator : comparator.reversed())
                .orElseThrow(() -> new IllegalArgumentException(guName + "의 평단가 데이터가 없습니다."));
        SingleRegionFacts facts = new SingleRegionFacts(guName + " " + selected.name(),
                selected.avg_thing_amt() == null ? 0 : selected.avg_thing_amt(), selected.avg_pyeong_amt(),
                selected.total_count() == null ? 0 : selected.total_count(), list.baseDate(), "만원", "만원/평");
        String direction = lowest ? "가장 낮은" : "가장 높은";
        SingleRegionPriceRequest request = new SingleRegionPriceRequest("top-bottom-search", question, facts,
                List.of(guName + "에서 평단가가 " + direction + " 동은 " + selected.name()),
                List.of("가격 예측", "투자 추천"));
        return aiClient.post().uri("/ai/explain-single-region").body(request).retrieve()
                .body(SingleRegionPriceResponse.class);
    }
}
