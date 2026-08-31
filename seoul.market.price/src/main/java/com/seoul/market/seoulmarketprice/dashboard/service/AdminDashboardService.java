package com.seoul.market.seoulmarketprice.dashboard.service;

import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardRepository;
import com.seoul.market.seoulmarketprice.dashboard.dto.AdminDashboardSummaryResponse;
import com.seoul.market.seoulmarketprice.qna.repository.QnaRepository;
import com.seoul.market.seoulmarketprice.pageview.service.PageViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 백오피스 대시보드에 필요한 집계 지표를 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final QnaRepository qnaRepository;
    private final PageViewService pageViewService;

    public AdminDashboardSummaryResponse getSummary() {
        OffsetDateTime generatedAt = OffsetDateTime.now(SEOUL_ZONE);
        LocalDate baseDate = generatedAt.toLocalDate();
        LocalDateTime from = baseDate.atStartOfDay();
        LocalDateTime to = baseDate.plusDays(1).atStartOfDay();

        long totalUserCount = memberRepository.countActiveUsers();
        long todayUserCount = memberRepository.countActiveUsersCreatedBetween(from, to);
        long totalBoardPostCount = boardRepository.countActivePosts();
        long todayBoardPostCount = boardRepository.countActivePostsCreatedBetween(from, to);
        long totalQnaPostCount = qnaRepository.countActivePosts();
        long todayQnaPostCount = qnaRepository.countActivePostsCreatedBetween(from, to);
        long todayPageViewCount = pageViewService.getTodayPageViewCount();

        return new AdminDashboardSummaryResponse(
                totalUserCount,
                todayUserCount,
                totalBoardPostCount,
                todayBoardPostCount,
                totalQnaPostCount,
                todayQnaPostCount,
                todayBoardPostCount + todayQnaPostCount,
                todayPageViewCount,
                baseDate,
                generatedAt
        );
    }
}
