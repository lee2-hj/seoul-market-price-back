package com.seoul.market.seoulmarketprice.ai.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceAdapterRegistryTest {
    @Test
    void selectsOnlyTheAdapterThatSupportsResolvedScope() {
        DataSourceAdapter districtAdapter = new StubAdapter(SearchScope.Type.ALL_SEOUL);
        DataSourceAdapter dongAdapter = new StubAdapter(SearchScope.Type.DONG);
        DataSourceAdapterRegistry registry = new DataSourceAdapterRegistry(List.of(districtAdapter, dongAdapter));

        assertThat(registry.find(new SearchScope(SearchScope.Type.DONG, "강남구", "대치동", null, null)))
                .contains(dongAdapter);
    }

    private record StubAdapter(SearchScope.Type supported) implements DataSourceAdapter {
        @Override public boolean supports(SearchScope scope) { return supported == scope.type(); }
        @Override public List<MetricRecord> fetch(SearchScope scope) { return List.of(); }
    }
}
