package com.seoul.market.seoulmarketprice.menus.service;

import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCategoryCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCategoryRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.MenuCategoryResponse;
import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.repository.MenuCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;

    //메뉴 카테고리 조회
    public MenuCategoryResponse getMenuList(MenuCategoryRequest request) {
        int page = (request == null || request.page() == null) ? 1 : request.page();
        int size = (request == null || request.size() == null) ? 10 : request.size();
        int zeroBasedPage = page - 1;
        long offset = (long) zeroBasedPage * size;

        boolean hasSearch = request != null &&
                (org.springframework.util.StringUtils.hasText(request.menuCode()) ||
                        org.springframework.util.StringUtils.hasText(request.menuName()));

        if(hasSearch){
            List<MenuCategoryResponse.MenuInfo> list= menuCategoryRepository
                    .findCodeOrName(request.menuCode(),request.menuName(), offset, size)
                    .stream()
                    .map(item -> new MenuCategoryResponse.MenuInfo(item.getId(),
                            item.getMenuCode(),
                            item.getMenuName(),
                            item.getCreated_at(),
                            item.getUpdated_at()))
                    .toList();
            long total = menuCategoryRepository.countMenuCodeOrMenuName(request.menuCode(),request.menuName());
            return MenuCategoryResponse.of(list,total,page, size);
        } else {
            Page<MenuCategoryEntity> Menupage = menuCategoryRepository.findAll(PageRequest.of(zeroBasedPage, size));
            List<MenuCategoryResponse.MenuInfo>list = Menupage.getContent().stream()
                    .map(item -> new MenuCategoryResponse.MenuInfo(item.getId(),
                            item.getMenuCode(),
                            item.getMenuName(),
                            item.getCreated_at(),
                            item.getUpdated_at()))
                    .toList();
            return MenuCategoryResponse.of(list,Menupage.getTotalPages(),page, size);
        }
    }

    //메뉴 카테고리 등록
    @Transactional
    public void createMenuCategory(MenuCategoryCreateRequest request) {
        menuCategoryRepository.save(new MenuCategoryEntity(request.menuCode(),request.menuName()));
    }

    //메뉴 카테고리 수정
    @Transactional
    public void updateMenuCategory(Long id, MenuCategoryCreateRequest request) {
       MenuCategoryEntity category = menuCategoryRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));

        category.updateMenuCategory(request.menuCode(), request.menuName());
    }

    //메뉴 카테고리 삭제
    @Transactional
    public void deleteCategory(Long id) {

        MenuCategoryEntity category = menuCategoryRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));

        menuCategoryRepository.delete(category);
    }
}
