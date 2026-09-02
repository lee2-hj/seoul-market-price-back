package com.seoul.market.seoulmarketprice.menus.controller;

import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCategoryCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCategoryRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.MenuCategoryResponse;
import com.seoul.market.seoulmarketprice.menus.service.MenuCategoryService;
import com.seoul.market.seoulmarketprice.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menuCategory")
@RequiredArgsConstructor
@Tag(name = "메뉴 카테고리", description = "메뉴 카테고리 관리")
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;

    @GetMapping()
    @Operation(summary = "메뉴 카테고리 리스트", description = "메뉴 카테고리 목록 조회 api")
    public ResponseEntity<MenuCategoryResponse> menuList(@ModelAttribute MenuCategoryRequest request){
        MenuCategoryResponse menuList = menuCategoryService.getMenuList(request);

        return ResponseEntity.status(HttpStatus.OK).body(menuList);
    }

    @PostMapping()
    @Operation(summary = "메뉴 카테고리 등록", description = "메뉴 카테고리 등록 api")
    public ResponseEntity<Void> createMenuCategory(@Valid @RequestBody MenuCategoryCreateRequest request){
        menuCategoryService.createMenuCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "메뉴 카테고리 수정", description = "메뉴 카테고리 수정 api")
    public ResponseEntity<Void> updateMenuCategory(@PathVariable Long id,
                                                   @Valid @RequestBody MenuCategoryCreateRequest request){
        menuCategoryService.updateMenuCategory(id,request);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "메뉴 카테고리 삭제", description = "메뉴 카테고리 삭제 api")
    public ResponseEntity<Void> deleteMenuCategory(@PathVariable Long id){

        menuCategoryService.deleteCategory(id);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
