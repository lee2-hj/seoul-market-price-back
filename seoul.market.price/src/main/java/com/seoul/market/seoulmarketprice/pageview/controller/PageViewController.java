package com.seoul.market.seoulmarketprice.pageview.controller;

import com.seoul.market.seoulmarketprice.pageview.service.PageViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/page-views")
@RequiredArgsConstructor
public class PageViewController {

    private final PageViewService pageViewService;

    @PostMapping
    public ResponseEntity<Void> recordPageView() {
        pageViewService.recordPageView();
        return ResponseEntity.noContent().build();
    }
}
