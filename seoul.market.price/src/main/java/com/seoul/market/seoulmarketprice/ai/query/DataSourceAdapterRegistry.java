package com.seoul.market.seoulmarketprice.ai.query;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Selects the first data source that can supply factual rows for a resolved scope. */
@Component
public class DataSourceAdapterRegistry {
    private final List<DataSourceAdapter> adapters;

    public DataSourceAdapterRegistry(List<DataSourceAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public Optional<DataSourceAdapter> find(SearchScope scope) {
        return adapters.stream().filter(adapter -> adapter.supports(scope)).findFirst();
    }
}
