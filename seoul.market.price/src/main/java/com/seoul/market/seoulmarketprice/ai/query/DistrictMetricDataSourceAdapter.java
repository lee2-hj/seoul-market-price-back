package com.seoul.market.seoulmarketprice.ai.query;

import com.seoul.market.seoulmarketprice.fastapi.dto.request.ListRequest;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.ListResponse;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import com.seoul.market.seoulmarketprice.location.entity.SggMaster;
import com.seoul.market.seoulmarketprice.location.repository.SggMasterRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Existing district FastAPI aggregation, exposed as common metric rows. */
@Component
public class DistrictMetricDataSourceAdapter implements DataSourceAdapter {
    private final SggMasterRepository sggRepository;
    private final FastApiService fastApiService;

    public DistrictMetricDataSourceAdapter(SggMasterRepository sggRepository, FastApiService fastApiService) {
        this.sggRepository = sggRepository;
        this.fastApiService = fastApiService;
    }

    @Override
    public boolean supports(SearchScope scope) {
        return scope.type() == SearchScope.Type.ALL_SEOUL;
    }

    @Override
    public List<MetricRecord> fetch(SearchScope scope) {
        return sggRepository.findAllByOrderBySggNameAsc().parallelStream()
                .map(this::summarize)
                .filter(record -> record != null)
                .toList();
    }

    private MetricRecord summarize(SggMaster sgg) {
        ListResponse response = fastApiService.getPyeongList(new ListRequest(sgg.getSggCode()));
        if (response == null || response.groups() == null) return null;

        long weightedSum = 0;
        long countSum = 0;
        for (ListResponse.ListSummaryDto item : response.groups().values()) {
            long count = item.total_count() == null ? 0 : item.total_count();
            if (item.avg_pyeong_amt() != null && count > 0) {
                weightedSum += item.avg_pyeong_amt() * count;
                countSum += count;
            }
        }
        if (countSum == 0) return null;
        long average = Math.round((double) weightedSum / countSum);
        return new MetricRecord("district:" + sgg.getSggCode(), sgg.getSggName(), null, null,
                null, average, null, null, countSum, null, response.baseDate());
    }
}
