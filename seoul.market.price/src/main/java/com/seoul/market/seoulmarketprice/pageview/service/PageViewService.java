package com.seoul.market.seoulmarketprice.pageview.service;

import com.seoul.market.seoulmarketprice.pageview.repository.PageViewDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PageViewService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private final PageViewDailyRepository repository;

    @Transactional
    public void recordPageView() {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        repository.increment(now.toLocalDate(), now);
    }

    @Transactional(readOnly = true)
    public long getTodayPageViewCount() {
        return repository.countByDate(LocalDate.now(SEOUL_ZONE));
    }
}
