package com.seoul.market.seoulmarketprice.fastapi.controller;

import com.seoul.market.seoulmarketprice.fastapi.dto.request.*;
import com.seoul.market.seoulmarketprice.fastapi.dto.response.*;
import com.seoul.market.seoulmarketprice.fastapi.service.FastApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "fastApi 호출관리", description = "fastApi 호출관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/fastApi")
public class FastApiController {

    private final FastApiService fastApiService;

    @Operation(summary = "지역별 평균가 비교", description = "지역별 평균가 비교 데이터 api")
    @GetMapping("/compare")
    public ResponseEntity<CompareResponse> compare(@Valid @ModelAttribute CompareRequest request){
        CompareResponse response = fastApiService.getCompare(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "지역별 평균가 비교(지도용)", description = "지역별 평균가 비교 데이터 api")
    @GetMapping("/list")
    public ResponseEntity<ListResponse> pyeongList(@Valid @ModelAttribute ListRequest request){
        ListResponse response = fastApiService.getPyeongList(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "지역내 아파트 가격 상위 5개,하위 5개", description = "지역내 아파트 가격 상위5개, 하위5개 데이터 api")
    @GetMapping("/topandbottom")
    public ResponseEntity<TopAndBottomResponse> topAndbottom(@Valid @ModelAttribute TopAndBottomRequest request){
        TopAndBottomResponse response = fastApiService.getTopAndBottom(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "아파트 타입별 비교(평형,층수)", description = "아파트 층수별 비교 api <br>" +
            "- 평형별 비교  api 'pyeong'=평단가,'floor'=층별가 <br>" +
            "- query_type='pyeong'이면 10/20/30/40 중 하나(40은 40평 이상 포함), query_type='floor'이면 LOW/MID/HIGH 중 하나")
    @GetMapping("/aptcompare")
    public ResponseEntity<AptCompareResponse> aptcompare(@Valid @ ModelAttribute AptCompareRequest request){
        AptCompareResponse response = fastApiService.getAptCompare(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "지역별 거래 동향", description = "지역별 거래 동향 api")
    @GetMapping("/rtt")
    public ResponseEntity<RttRespopnse> rttInfo(@Valid @ModelAttribute RttRequest request){
        RttRespopnse response = fastApiService.getRttInfo(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
