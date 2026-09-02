package com.seoul.market.seoulmarketprice.menus.service;

import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.MenuRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.MenuResponse;
import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.repository.MenuCategoryRepository;
import com.seoul.market.seoulmarketprice.menus.repository.MenuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    //메뉴 리스트 조회
    public MenuResponse getMenusList(MenuRequest request) {
        int page = (request == null || request.page() == null) ? 1 : request.page();
        int size = (request == null || request.size() == null) ? 10 : request.size();
        int zeroBasedPage = page - 1;
        long offset = (long) zeroBasedPage * size;

        boolean hasSearch = request != null &&
                (org.springframework.util.StringUtils.hasText(request.menuCategoryCode()) ||
                        org.springframework.util.StringUtils.hasText(request.menuCode()) ||
                        org.springframework.util.StringUtils.hasText(request.menuName()));

        if (hasSearch) {
            List<MenuResponse.Menus> list = menuRepository
                    .findmenuCategoryCodeOrmenuCodeOrmenuName(request.menuCategoryCode(),
                            request.menuCode(),
                            request.menuName(),
                            offset, size)
                    .stream()
                    .map(item -> new MenuResponse.Menus(
                            item.getId(),
                            item.getCategory().getMenuCode(),
                            item.getCategory().getMenuName(),
                            item.getMenuCode(),
                            item.getMenuName(),
                            item.getUrl(),
                            item.getCreated_at(),
                            item.getUpdated_at()
                    ))
                    .toList();
            long total = menuRepository.countmenuCategoryCodeOrmenuCodeOrmenuName(request.menuCategoryCode(),
                    request.menuCode(),
                    request.menuName()
                    );
            return MenuResponse.of(list,total,page, size);
        }else{
            Page<MenuEntity> MenuPage = menuRepository.findAll(PageRequest.of(zeroBasedPage, size));
            List<MenuResponse.Menus> list = MenuPage.getContent().stream()
                    .map(item -> new MenuResponse.Menus(
                            item.getId(),
                            item.getCategory().getMenuCode(),
                            item.getCategory().getMenuName(),
                            item.getMenuCode(),
                            item.getMenuName(),
                            item.getUrl(),
                            item.getCreated_at(),
                            item.getUpdated_at()
                    ))
                    .toList();
            return MenuResponse.of(list, MenuPage.getTotalPages(), page, size);
        }
    }

    // 메뉴 등록
    @Transactional
    public void createMenu(@Valid MenuCreateRequest request) {
       if(menuRepository.existsByMenuCode(request.menuCode())){
           throw new IllegalArgumentException("이미 존재하는 메뉴코드입니다.");
       }

        MenuCategoryEntity category = menuCategoryRepository.findByMenuCode(request.menuCategoryCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));

        menuRepository.save(new MenuEntity(
                category,
                request.menuCode(),
                request.menuName(),
                request.url()
        ));

    }

    //메뉴 수정
    @Transactional
    public void updateMenu(Long id, @Valid MenuCreateRequest request) {
        MenuEntity menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

        MenuCategoryEntity category = menuCategoryRepository.findByMenuCode(request.menuCategoryCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));

        menu.updateMenu(
                category,
                request.menuCode(),
                request.menuName(),
                request.url()
        );
    }

    //메뉴 삭제
    @Transactional
    public void deleteMenu(Long id) {
        MenuEntity menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다."));

        menuRepository.delete(menu);
    }
}
