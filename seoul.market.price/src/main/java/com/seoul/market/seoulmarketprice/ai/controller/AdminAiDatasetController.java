package com.seoul.market.seoulmarketprice.ai.controller;

import com.seoul.market.seoulmarketprice.ai.repository.ApartmentLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/datasets")
@RequiredArgsConstructor
public class AdminAiDatasetController {
    private final ApartmentLocationRepository apartmentLocationRepository;

    @PostMapping("/apartment-main/refresh")
    public ResponseEntity<ApartmentLocationRepository.DatasetRefreshResult> refreshApartmentMain() {
        return ResponseEntity.ok(apartmentLocationRepository.refresh());
    }
}
