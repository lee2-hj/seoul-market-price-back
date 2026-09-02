package com.seoul.market.seoulmarketprice.menus.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.member.exception.AdminNotFoundException;
import com.seoul.market.seoulmarketprice.menus.dto.request.ActiveMenuCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.ActiveMenuDeleteRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.ActiveMenuResponse;
import com.seoul.market.seoulmarketprice.menus.entity.ActiveMenuEntity;
import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.exception.ActiveMenuAccessDeniedException;
import com.seoul.market.seoulmarketprice.menus.repository.ActiveMenuRepository;
import com.seoul.market.seoulmarketprice.menus.repository.MenuCategoryRepository;
import com.seoul.market.seoulmarketprice.menus.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자별로 접근 가능한 메뉴(활성 메뉴)를 조회, 등록, 해제하는 서비스이다.
 *
 * <p>
 * {@code /me} 계열 호출은 principal에서 얻은 자기 자신의 고유번호만 사용하므로
 * 구조적으로 IDOR이 발생할 수 없다. {@code /{id}} 계열 호출은 MASTER 전용
 * 관리 기능으로, SecurityConfig의 인가 규칙이 1차 방어선이며, 여기서 수행하는
 * {@link #assertAccessible(Long, Long, boolean)} 검증이 2차 방어선이다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActiveMenuService {

    private final ActiveMenuRepository activeMenuRepository;
    private final AdminRepository adminRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuRepository menuRepository;

    /** 로그인한 본인의 활성 메뉴를 조회한다({@code /me} 전용, IDOR 불가). */
    public List<ActiveMenuResponse> getActiveMenu(Long id) {
        return getActiveMenu(id, id, true);
    }

    /**
     * 대상 관리자의 활성 메뉴를 조회한다.
     *
     * @param id                조회 대상 관리자 고유번호
     * @param requesterId       요청자(로그인한 관리자) 고유번호
     * @param requesterIsMaster 요청자가 MASTER 권한인지 여부
     */
    public List<ActiveMenuResponse> getActiveMenu(Long id, Long requesterId, boolean requesterIsMaster) {
        assertAccessible(id, requesterId, requesterIsMaster);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(AdminNotFoundException::new);

        List<ActiveMenuEntity> list = activeMenuRepository.findByAdminId(id);

        return list.stream()
                .map(item -> new ActiveMenuResponse(
                        item.getId(),
                        admin.getId(),
                        item.getCategory().getMenuCode(),
                        item.getCategory().getMenuName(),
                        item.getMenu().getMenuCode(),
                        item.getMenu().getMenuName(),
                        item.getMenu().getUrl(),
                        item.getCreated_at(),
                        item.getUpdated_at()
                ))
                .toList();
    }

    /** 대상 관리자에게 활성 메뉴를 등록한다(MASTER 전용 관리 기능). */
    @Transactional
    public void createActiveMenu(Long id, ActiveMenuCreateRequest request, Long requesterId, boolean requesterIsMaster) {
        assertAccessible(id, requesterId, requesterIsMaster);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(AdminNotFoundException::new);

        List<ActiveMenuEntity> actives = request.actives().stream()
                .map(item -> {
                    MenuCategoryEntity category = menuCategoryRepository.findByMenuCode(item.categoryCode())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));

                    MenuEntity menu = menuRepository
                            .findByCategory_MenuCodeAndMenuCode(item.categoryCode(), item.menuCode())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

                    return new ActiveMenuEntity(admin, category, menu);
                })
                .toList();

        activeMenuRepository.saveAll(actives);
    }

    /** 대상 관리자의 활성 메뉴를 해제한다(MASTER 전용 관리 기능). */
    @Transactional
    public void deleteActiveMenu(Long id, ActiveMenuDeleteRequest request, Long requesterId, boolean requesterIsMaster) {
        assertAccessible(id, requesterId, requesterIsMaster);

        if (!adminRepository.existsById(id)) {
            throw new AdminNotFoundException();
        }

        List<ActiveMenuEntity> actives = request.activeMenuIds().stream()
                .map(activeMenuId -> activeMenuRepository.findById(activeMenuId)
                        .orElseThrow(() -> new IllegalArgumentException("이미 사용하지 않는 메뉴입니다.")))
                .toList();

        activeMenuRepository.deleteAllInBatch(actives);
    }

    /**
     * MASTER가 아닌 요청자가 자기 자신이 아닌 다른 관리자의 활성 메뉴에
     * 접근하지 못하도록 서비스 레벨에서 한 번 더 검증한다(IDOR 2차 방어선).
     */
    private void assertAccessible(Long targetId, Long requesterId, boolean requesterIsMaster) {
        if (!requesterIsMaster && !targetId.equals(requesterId)) {
            throw new ActiveMenuAccessDeniedException();
        }
    }
}
