package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.member.dto.request.admin.AdminUpdateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminUpdateResponse;
import com.seoul.market.seoulmarketprice.member.exception.AdminDeletionException;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
import com.seoul.market.seoulmarketprice.member.repository.AdminCreationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 관리자 수정 및 삭제 서비스의 단위 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {

    @Mock
    private AdminCreationRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private AdminManagementServiceImpl service;

    /** 각 테스트에서 독립적인 서비스 객체를 생성한다. */
    @BeforeEach
    void setUp() {
        service = new AdminManagementServiceImpl(repository, passwordEncoder);
    }

    /** 비밀번호를 포함한 수정 요청은 암호화된 값으로 저장되는지 검증한다. */
    @Test
    void updateAdminEncodesPasswordAndUpdatesRequestedFields() {
        AdminUpdateRequest request = new AdminUpdateRequest(
                "newPassword123!", "새관리자", "010-1234-5678", "admin@example.com"
        );
        AdminUpdateResponse expected = new AdminUpdateResponse(
                2L, "admin02", "새관리자", "010-1234-5678",
                "admin@example.com", LocalDateTime.now()
        );
        when(passwordEncoder.encode("newPassword123!")).thenReturn("encoded-password");
        when(repository.update(
                2L, "encoded-password", "새관리자", "010-1234-5678", "admin@example.com"
        )).thenReturn(Optional.of(expected));

        AdminUpdateResponse result = service.updateAdmin(2L, request);

        assertThat(result).isEqualTo(expected);
        verify(repository).update(
                2L, "encoded-password", "새관리자", "010-1234-5678", "admin@example.com"
        );
    }

    /** 빈 수정 요청은 저장소 호출 전에 거부되는지 검증한다. */
    @Test
    void updateAdminRejectsEmptyRequest() {
        AdminUpdateRequest request = new AdminUpdateRequest(null, null, null, null);

        assertThatThrownBy(() -> service.updateAdmin(2L, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).update(2L, null, null, null, null);
    }

    /** 존재하지 않는 관리자의 수정은 전용 예외로 변환되는지 검증한다. */
    @Test
    void updateAdminRejectsMissingAdmin() {
        AdminUpdateRequest request = new AdminUpdateRequest(null, "새관리자", null, null);
        when(repository.update(2L, null, "새관리자", null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAdmin(2L, request))
                .isInstanceOf(AdminNotFoundException.class);
    }

    /** 정상 삭제 요청은 소프트 삭제 저장소 기능을 호출하는지 검증한다. */
    @Test
    void deleteAdminSoftDeletesTarget() {
        when(repository.existsActiveById(2L)).thenReturn(true);
        when(repository.countAll()).thenReturn(2L);
        when(repository.softDelete(2L)).thenReturn(1);

        service.deleteAdmin(2L, 1L);

        verify(repository).softDelete(2L);
    }

    /** 현재 로그인한 관리자 자신의 삭제는 거부되는지 검증한다. */
    @Test
    void deleteAdminRejectsCurrentAdmin() {
        when(repository.existsActiveById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAdmin(1L, 1L))
                .isInstanceOf(AdminDeletionException.class)
                .hasMessage("현재 로그인한 관리자 계정은 삭제할 수 없습니다.");
        verify(repository, never()).softDelete(1L);
    }
}
