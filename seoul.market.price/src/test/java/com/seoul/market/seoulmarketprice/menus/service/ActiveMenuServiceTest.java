package com.seoul.market.seoulmarketprice.menus.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
import com.seoul.market.seoulmarketprice.menus.dto.response.ActiveMenuResponse;
import com.seoul.market.seoulmarketprice.menus.entity.ActiveMenuEntity;
import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.exception.ActiveMenuAccessDeniedException;
import com.seoul.market.seoulmarketprice.menus.repository.ActiveMenuRepository;
import com.seoul.market.seoulmarketprice.menus.repository.MenuCategoryRepository;
import com.seoul.market.seoulmarketprice.menus.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 활성 메뉴 조회 서비스의 정상 동작과 IDOR 방어 로직을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ActiveMenuServiceTest {

    @Mock
    private ActiveMenuRepository activeMenuRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private MenuCategoryRepository menuCategoryRepository;
    @Mock
    private MenuRepository menuRepository;

    private ActiveMenuService service;

    @BeforeEach
    void setUp() {
        service = new ActiveMenuService(
                activeMenuRepository, adminRepository, menuCategoryRepository, menuRepository
        );
    }

    private ActiveMenuEntity newActiveMenuEntity(Admin admin, Long id) {
        MenuCategoryEntity category = new MenuCategoryEntity("CAT01", "카테고리1");
        MenuEntity menu = new MenuEntity(category, "MENU01", "메뉴1", "/menu1");
        ActiveMenuEntity activeMenu = new ActiveMenuEntity(admin, category, menu);
        ReflectionTestUtils.setField(activeMenu, "id", id);
        ReflectionTestUtils.setField(activeMenu, "created_at", LocalDateTime.of(2024, 1, 1, 0, 0));
        ReflectionTestUtils.setField(activeMenu, "updated_at", LocalDateTime.of(2024, 1, 2, 0, 0));
        return activeMenu;
    }

    /** 본인 조회(/me)는 응답에 url 필드가 포함되고 대상 관리자의 활성 메뉴를 그대로 반환하는지 검증한다. */
    @Test
    void getActiveMenuReturnsActiveMenusIncludingUrl() {
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(activeMenuRepository.findByAdminId(1L))
                .thenReturn(List.of(newActiveMenuEntity(admin, 10L)));

        List<ActiveMenuResponse> result = service.getActiveMenu(1L);

        assertThat(result).hasSize(1);
        ActiveMenuResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.adminId()).isEqualTo(1L);
        assertThat(response.categoryCode()).isEqualTo("CAT01");
        assertThat(response.categoryName()).isEqualTo("카테고리1");
        assertThat(response.menuCode()).isEqualTo("MENU01");
        assertThat(response.menuName()).isEqualTo("메뉴1");
        assertThat(response.url()).isEqualTo("/menu1");
    }

    /** 존재하지 않는 관리자를 조회하면 전용 예외가 발생하는지 검증한다. */
    @Test
    void getActiveMenuRejectsMissingAdmin() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveMenu(99L))
                .isInstanceOf(AdminNotFoundException.class);
    }

    /** MASTER가 아닌 요청자가 자기 자신이 아닌 다른 관리자를 조회하면 거부되는지 검증한다(IDOR 2차 방어). */
    @Test
    void getActiveMenuRejectsNonMasterAccessingOthers() {
        assertThatThrownBy(() -> service.getActiveMenu(2L, 1L, false))
                .isInstanceOf(ActiveMenuAccessDeniedException.class);

        verify(adminRepository, never()).findById(any());
    }

    /** MASTER는 다른 관리자의 활성 메뉴도 조회할 수 있는지 검증한다. */
    @Test
    void getActiveMenuAllowsMasterAccessingOthers() {
        Admin admin = mock(Admin.class);
        when(adminRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(activeMenuRepository.findByAdminId(2L)).thenReturn(List.of());

        List<ActiveMenuResponse> result = service.getActiveMenu(2L, 1L, true);

        assertThat(result).isEmpty();
    }
}
