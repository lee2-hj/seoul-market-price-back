package com.seoul.market.seoulmarketprice.location.controller;

import com.seoul.market.seoulmarketprice.location.dto.CurrentDistrictResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "위치", description = "현재 좌표 기반 지역 조회 API")
@RestController
@RequestMapping("/api/location")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "현재 좌표의 서울 자치구 조회")
    @GetMapping("/current-district")
    public CurrentDistrictResponse getCurrentDistrict(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return locationService.findCurrentDistrict(latitude, longitude);
    }
}
