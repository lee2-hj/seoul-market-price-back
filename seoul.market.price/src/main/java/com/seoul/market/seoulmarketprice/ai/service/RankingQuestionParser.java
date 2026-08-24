package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.RankingMetric;
import com.seoul.market.seoulmarketprice.ai.dto.RankingSearchQuery;
import com.seoul.market.seoulmarketprice.ai.dto.RankingTarget;
import com.seoul.market.seoulmarketprice.ai.dto.SortDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 순위형 한국어 질문을 데이터 조회에 사용할 공통 조건으로 변환한다. */
@Component
public class RankingQuestionParser {
    private static final int DEFAULT_LIMIT = 5;
    private static final int DEFAULT_MINIMUM_TRADE_COUNT = 3;
    private static final BigDecimal SHARP_DROP_THRESHOLD = BigDecimal.valueOf(-10);
    private static final BigDecimal SHARP_RISE_THRESHOLD = BigDecimal.valueOf(10);
    private static final BigDecimal EOK_IN_WON = BigDecimal.valueOf(100_000_000L);

    private static final Pattern RECENT_MONTHS = Pattern.compile("최근\\s*(\\d+)\\s*개월");
    private static final Pattern RECENT_WEEKS = Pattern.compile("최근\\s*(\\d+)\\s*주");
    private static final Pattern RECENT_DAYS = Pattern.compile("최근\\s*(\\d+)\\s*일");
    private static final Pattern LIMIT = Pattern.compile("(?:상위|top)\\s*(\\d+)\\s*(?:개|곳|단지)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern EOK_RANGE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억\\s*(?:~|-|부터)\\s*(\\d+(?:\\.\\d+)?)\\s*억");
    private static final Pattern EOK_MAX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억\\s*(?:이하|미만)");
    private static final Pattern EOK_MIN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억\\s*(?:이상|초과)");

    private final Clock clock;

    public RankingQuestionParser() {
        this(Clock.systemDefaultZone());
    }

    RankingQuestionParser(Clock clock) {
        this.clock = clock;
    }

    public RankingSearchQuery parse(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("질문을 입력해 주세요.");
        }

        RankingMetric metric = metric(question);
        LocalDate today = LocalDate.now(clock);
        PriceRange priceRange = priceRange(question);

        return new RankingSearchQuery(
                RankingTarget.APARTMENT,
                metric,
                direction(question, metric),
                null,
                from(question, today),
                today,
                priceRange.minPrice(),
                priceRange.maxPrice(),
                threshold(question, metric),
                limit(question),
                DEFAULT_MINIMUM_TRADE_COUNT
        );
    }

    private RankingMetric metric(String question) {
        if (containsAny(question, "하락률", "상승률", "급락", "급상승", "떨어", "올라", "상승", "하락")) {
            return RankingMetric.CHANGE_RATE;
        }
        if (containsAny(question, "거래량", "거래 많은", "거래가 많은")) return RankingMetric.TRADE_COUNT;
        if (containsAny(question, "인기", "조회수", "관심 등록")) return RankingMetric.POPULARITY;
        if (containsAny(question, "가격대", "억 이하", "억 이상", "억 미만", "억 초과", "비싼", "비싸", "고가", "저렴", "싼", "평당가", "평단가")) {
            return RankingMetric.PRICE;
        }
        throw new IllegalArgumentException("순위 기준(상승률, 하락률, 거래량, 인기, 가격대)을 찾지 못했습니다.");
    }

    private SortDirection direction(String question, RankingMetric metric) {
        if (metric == RankingMetric.CHANGE_RATE) {
            return containsAny(question, "하락", "급락", "떨어", "낮아") ? SortDirection.ASC : SortDirection.DESC;
        }
        if (metric == RankingMetric.PRICE) {
            boolean lowPriceExpression = containsAny(question, "이하", "미만", "저렴", "낮은", "최저")
                    || (question.contains("싼") && !question.contains("비싼"));
            return lowPriceExpression ? SortDirection.ASC : SortDirection.DESC;
        }
        return SortDirection.DESC;
    }

    private BigDecimal threshold(String question, RankingMetric metric) {
        if (metric != RankingMetric.CHANGE_RATE) return null;
        if (question.contains("급락")) return SHARP_DROP_THRESHOLD;
        if (question.contains("급상승")) return SHARP_RISE_THRESHOLD;
        return null;
    }

    private LocalDate from(String question, LocalDate today) {
        Matcher months = RECENT_MONTHS.matcher(question);
        if (months.find()) return today.minusMonths(Integer.parseInt(months.group(1)));
        Matcher weeks = RECENT_WEEKS.matcher(question);
        if (weeks.find()) return today.minusWeeks(Integer.parseInt(weeks.group(1)));
        Matcher days = RECENT_DAYS.matcher(question);
        if (days.find()) return today.minusDays(Integer.parseInt(days.group(1)));
        if (question.contains("올해")) return today.withDayOfYear(1);
        return today.minusMonths(1);
    }

    private int limit(String question) {
        Matcher matcher = LIMIT.matcher(question);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : DEFAULT_LIMIT;
    }

    private PriceRange priceRange(String question) {
        Matcher range = EOK_RANGE.matcher(question);
        if (range.find()) return new PriceRange(toWon(range.group(1)), toWon(range.group(2)));
        Matcher max = EOK_MAX.matcher(question);
        if (max.find()) return new PriceRange(null, toWon(max.group(1)));
        Matcher min = EOK_MIN.matcher(question);
        if (min.find()) return new PriceRange(toWon(min.group(1)), null);
        return new PriceRange(null, null);
    }

    private BigDecimal toWon(String eok) {
        return new BigDecimal(eok).multiply(EOK_IN_WON);
    }

    private boolean containsAny(String question, String... keywords) {
        for (String keyword : keywords) {
            if (question.contains(keyword)) return true;
        }
        return false;
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {}
}
