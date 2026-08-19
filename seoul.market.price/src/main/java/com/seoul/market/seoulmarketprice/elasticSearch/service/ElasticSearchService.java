package com.seoul.market.seoulmarketprice.elasticSearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.request.AptNameRequest;
import com.seoul.market.seoulmarketprice.elasticSearch.dto.response.AptNameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticSearchService {

    private static final String INDEX = "apt_name";
    // Elasticsearch는 size에 진짜 "무제한"을 줄 수 없고, 인덱스 기본 설정
    // index.max_result_window(기본값 10000)가 사실상의 상한이다. 이 인덱스는
    // 그 설정을 따로 늘리지 않았으므로, 그 값을 그대로 써서 실질적으로 제한이 없도록 한다.
    private static final int SEARCH_SIZE = 10000;
    private static final List<String> SOURCE_FIELDS = List.of("apt_name", "mno", "sno");

    private final ElasticsearchClient elasticsearchClient;

    // apt_name 자동완성 검색. apt_name이 비어있으면 이름 조건 없이 sgg_cd/dong_cd만으로 목록을
    // 불러온다. sgg_cd/dong_cd가 있으면(apt_name 입력 여부와 무관하게) 정확히 일치하는
    // 문서로만 필터링한다.
    public List<AptNameResponse> searchAptName(AptNameRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (StringUtils.hasText(request.apt_name())) {
            boolQuery.must(m -> m.matchPhrasePrefix(p -> p.field("apt_name").query(request.apt_name())));
        }

        if (StringUtils.hasText(request.sgg_cd())) {
            boolQuery.filter(f -> f.term(t -> t.field("sgg_cd").value(request.sgg_cd())));
        }
        if (StringUtils.hasText(request.dong_cd())) {
            boolQuery.filter(f -> f.term(t -> t.field("dong_cd").value(request.dong_cd())));
        }

        try {
            SearchResponse<AptNameResponse> response = elasticsearchClient.search(s -> s
                            .index(INDEX)
                            .size(SEARCH_SIZE)
                            .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS)))
                            .query(q -> q.bool(boolQuery.build())),
                    AptNameResponse.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("엘라스틱서치 apt_name 검색에 실패했습니다.", e);
        }
    }
}
