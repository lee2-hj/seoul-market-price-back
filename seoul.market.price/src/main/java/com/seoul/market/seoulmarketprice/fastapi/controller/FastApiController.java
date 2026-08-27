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

    @Operation(summary = "지역내 아파트 가격 상위 5개,하위 5개", description = "지역내 아파트 가격 상위5개, 하위5개 데이터 api<br>" +
            "정렬 기준. 'pyeong'=평균 평당가(avg_pyeong_amt), 'thing_amt'=평균 거래가(avg_thing_amt)")
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

    @Operation(summary ="아파트별 비교", description = "아파트별 비교 api<br>"+
            "아파트1(cgg_cd_1/bjd_cd_1/apt_nm_1/mno_1/sno_1)과 아파트2(cgg_cd_2/bjd_cd_2/apt_nm_2/mno_2/sno_2)를 <br>" +
            "각각 자치구코드+법정동코드+아파트명+지번 본번/부번(모두 필수)으로 특정하여, <br>" +
            "dm_apt_recent_trade 마트의 최신 파티션(최근 90일 실거래 사전집계)에서 <br>" +
            "평균 매매가/평균 평당가/거래건수와 세대수/준공년도/사용승인일을 조회해 aptGroup1/aptGroup2로 나누어 반환한다. <br>" +
            "매칭되는 단지가 없거나 최근 90일간 거래가 없으면 해당 그룹은 빈 객체({})로 반환된다(요청 자체는 404 처리하지 않음).<br>")
    @GetMapping("/regionaptcompare")
    public ResponseEntity<RegionAptCompareResponse> regionaptcompare(@Valid @ModelAttribute RegionAptCompareRequest request){
        RegionAptCompareResponse response = fastApiService.getRegionaptcompare(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "지역별 거래 동향", description = "지역별 거래 동향 api" )
    @GetMapping("/rtt")
    public ResponseEntity<RttRespopnse> rttInfo(@Valid @ModelAttribute RttRequest request){
        RttRespopnse response = fastApiService.getRttInfo(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "아파트 별 거래 동향", description = "아파트 별 거래 동향 api")
    @GetMapping("/aptmkt")
    public ResponseEntity<AptMktResponse> aptmktInfo(@Valid @ModelAttribute AptMktRequest request){
        AptMktResponse response = fastApiService.getAptmktInfo(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "메인 페이지", description = "메인 페이지 api")
    @GetMapping("/mainpage")
    public ResponseEntity<MainPageResponse> mainpageInfo(@Valid @ModelAttribute MainPageRequest request){
        MainPageResponse response = fastApiService.getMainpageInfo(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
