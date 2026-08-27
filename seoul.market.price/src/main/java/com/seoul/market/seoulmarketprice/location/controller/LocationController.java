package com.seoul.market.seoulmarketprice.location.controller;

import com.seoul.market.seoulmarketprice.location.dto.CurrentDistrictResponse;
import com.seoul.market.seoulmarketprice.location.dto.DongResponse;
import com.seoul.market.seoulmarketprice.location.dto.DongRegionResponse;
import com.seoul.market.seoulmarketprice.location.dto.SggResponse;
import com.seoul.market.seoulmarketprice.location.service.LocationService;
import com.seoul.market.seoulmarketprice.location.service.LocationMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "위치", description = "현재 좌표 기반 지역 조회 API")
@RestController
@RequestMapping("/api/location")
public class LocationController {
    private final LocationService locationService;
    private final LocationMasterService locationMasterService;

    public LocationController(
            LocationService locationService,
            LocationMasterService locationMasterService
    ) {
        this.locationService = locationService;
        this.locationMasterService = locationMasterService;
    }

    @Operation(summary = "현재 좌표의 서울 자치구 조회")
    @GetMapping("/current-district")
    public CurrentDistrictResponse getCurrentDistrict(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return locationService.findCurrentDistrict(latitude, longitude);
    }

    /** 회원가입 및 지역 선택 화면에 사용할 모든 자치구를 반환한다. */
    @Operation(summary = "전체 자치구 목록 조회")
    @GetMapping("/sggs")
    public List<SggResponse> getSggs() {
        return locationMasterService.getSggs();
    }

    /** 자치구 코드에 해당하는 행정동 목록을 반환한다. */
    @Operation(summary = "자치구별 행정동 목록 조회")
    @GetMapping("/dongs")
    public List<DongResponse> getDongs(@RequestParam String sggCd) {
        return locationMasterService.getDongs(sggCd);
    }

    @GetMapping("/resolve-dongs")
    public List<DongRegionResponse> resolveDongs(@RequestParam String dong1, @RequestParam String dong2) {
        return locationMasterService.resolveDongs(dong1, dong2);
    }

    @GetMapping("/resolve-dong")
    public List<DongRegionResponse> resolveDong(@RequestParam String dong) {
        return locationMasterService.resolveDong(dong);
    }
}
