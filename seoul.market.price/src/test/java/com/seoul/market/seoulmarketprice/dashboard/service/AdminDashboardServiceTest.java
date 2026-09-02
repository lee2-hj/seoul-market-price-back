package com.seoul.market.seoulmarketprice.dashboard.service;

import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.board.repository.BoardQueryRepository;
import com.seoul.market.seoulmarketprice.dashboard.dto.AdminDashboardSummaryResponse;
import com.seoul.market.seoulmarketprice.qna.repository.QnaRepository;
import com.seoul.market.seoulmarketprice.pageview.service.PageViewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock BoardQueryRepository boardQueryRepository;
    @Mock QnaRepository qnaRepository;
    @Mock PageViewService pageViewService;
    @InjectMocks AdminDashboardService adminDashboardService;

    @Test
    void 활성_회원과_삭제되지_않은_오늘_게시글을_집계한다() {
        when(memberRepository.countActiveUsers()).thenReturn(1_528L);
        when(memberRepository.countActiveUsersCreatedBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(12L);
        when(boardQueryRepository.countActivePosts()).thenReturn(320L);
        when(boardQueryRepository.countActivePostsCreatedBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(7L);
        when(qnaRepository.countActivePosts()).thenReturn(85L);
        when(qnaRepository.countActivePostsCreatedBetween(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(4L);
        when(pageViewService.getTodayPageViewCount()).thenReturn(99L);

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertThat(response.totalUserCount()).isEqualTo(1_528L);
        assertThat(response.todayUserCount()).isEqualTo(12L);
        assertThat(response.totalBoardPostCount()).isEqualTo(320L);
        assertThat(response.todayBoardPostCount()).isEqualTo(7L);
        assertThat(response.totalQnaPostCount()).isEqualTo(85L);
        assertThat(response.todayQnaPostCount()).isEqualTo(4L);
        assertThat(response.todayTotalPostCount()).isEqualTo(11L);
        assertThat(response.todayPageViewCount()).isEqualTo(99L);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(memberRepository).countActiveUsersCreatedBetween(fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue().toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
        assertThat(toCaptor.getValue()).isEqualTo(fromCaptor.getValue().plusDays(1));
    }
}
