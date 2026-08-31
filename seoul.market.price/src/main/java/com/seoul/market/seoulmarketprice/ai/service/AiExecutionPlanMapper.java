package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.AiExecutionPlan;
import com.seoul.market.seoulmarketprice.ai.dto.QuestionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiExecutionPlanMapper {
    public AiExecutionPlan map(QuestionAnalysisResponse analysis) {
        String city = null, district = null, dong = null;
        for (QuestionAnalysisResponse.AnalyzedRegion region : analysis.regions() == null ? List.<QuestionAnalysisResponse.AnalyzedRegion>of() : analysis.regions()) {
            if ("CITY".equals(region.type())) city = region.name();
            if ("DISTRICT".equals(region.type())) district = region.name();
            if ("DONG".equals(region.type())) dong = region.name();
        }
        QuestionAnalysisResponse.SearchFilters source = analysis.filters() == null
                ? new QuestionAnalysisResponse.SearchFilters(null, null, null, null) : analysis.filters();
        return new AiExecutionPlan(analysis.intent(),
                new AiExecutionPlan.Scope(city, district, dong,
                        analysis.referencePlace() == null ? null : analysis.referencePlace().name()),
                new AiExecutionPlan.Filters(source.minPyeong(), source.maxPyeong(), source.minPriceWon(),
                        source.maxPriceWon(), 3, null, null),
                new AiExecutionPlan.Sort(analysis.metric(), analysis.direction()),
                analysis.limit() == null ? 10 : analysis.limit(),
                analysis.toolPlan() == null ? List.of() : analysis.toolPlan(),
                Boolean.TRUE.equals(analysis.requiresClarification()),
                analysis.missingFields() == null ? List.of() : analysis.missingFields());
    }
}
