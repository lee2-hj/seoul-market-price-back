package com.seoul.market.seoulmarketprice.menus.controller;

import com.seoul.market.seoulmarketprice.menus.dto.request.MenuCreateRequest;
import com.seoul.market.seoulmarketprice.menus.dto.request.MenuRequest;
import com.seoul.market.seoulmarketprice.menus.dto.response.MenuAllResponse;
import com.seoul.market.seoulmarketprice.menus.dto.response.MenuResponse;
import com.seoul.market.seoulmarketprice.menus.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "메뉴 관리")
public class MenuController {

    private final MenuService menuService;

    @GetMapping()
    @Operation(summary = "메뉴 리스트 조회", description = "메뉴목록 조회 api")
    public ResponseEntity<MenuResponse> menusList(@ModelAttribute MenuRequest request){
        MenuResponse response = menuService.getMenusList(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/all")
    @Operation(summary = "모든 메뉴 리스트 조회", description = "모든 메뉴목록 조회 api")
    public ResponseEntity<MenuAllResponse> menusListAll(){
        MenuAllResponse response = menuService.getMenusListAll();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping()
    @Operation(summary = "메뉴 등록", description = "메뉴 등록 api")
    public ResponseEntity<Void> createMenu(@Valid @RequestBody MenuCreateRequest request){
        menuService.createMenu(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "메뉴 수정", description = "메뉴 수정 api")
    public ResponseEntity<Void> updateMenu(@PathVariable Long id,
                                           @Valid @RequestBody MenuCreateRequest request){
        menuService.updateMenu(id, request);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "메뉴 삭제", description = "메뉴 삭제 api")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id){
        menuService.deleteMenu(id);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
