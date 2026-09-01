package com.seoul.market.seoulmarketprice.ai.query;

import java.util.List;

/** Adapters hide FastAPI, Elasticsearch, Parquet, or JPA-specific retrieval details. */
public interface DataSourceAdapter {
    boolean supports(SearchScope scope);

    List<MetricRecord> fetch(SearchScope scope);
}
