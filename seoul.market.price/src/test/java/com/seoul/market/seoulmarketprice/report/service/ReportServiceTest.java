package com.seoul.market.seoulmarketprice.report.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.report.dto.request.ReportCreateRequest;
import com.seoul.market.seoulmarketprice.report.entity.Report;
import com.seoul.market.seoulmarketprice.report.entity.ReportCategory;
import com.seoul.market.seoulmarketprice.report.entity.ReportStatus;
import com.seoul.market.seoulmarketprice.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reportRepository;
    @Mock MemberManagementRepository memberRepository;

    @Test
    void createsReportOnlyForActiveMemberAndUsesAuthenticatedMemberId() {
        ReportService service = new ReportService(reportRepository, memberRepository);
        Member member = Member.createLocalMember("member1", "encoded", "홍길동", null, null,
                null, "010-1234-5678", null, (byte)1, (byte)1, (byte)1,
                null, null, null, null);
        when(memberRepository.findActiveById(7L)).thenReturn(Optional.of(member));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createReport(7L, new ReportCreateRequest(
                ReportCategory.FAKE_LISTING, "헬리오시티", "허위 매물 신고", "상세 내용", true));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(response.authorMemberId()).isEqualTo(7L);
        assertThat(response.authorName()).isEqualTo("홍*동");
    }

    @Test
    void rejectsReportWhenJwtMemberIsNotAnActiveRegisteredMember() {
        ReportService service = new ReportService(reportRepository, memberRepository);
        when(memberRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReport(99L, new ReportCreateRequest(
                ReportCategory.OTHER, "대상", "제목", "내용", false)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("가입된 회원만 신고할 수 있습니다.");
        verify(reportRepository, never()).save(any());
    }
}
