package com.example.egobook_be.domain.ads.controller;

import com.example.egobook_be.domain.ads.dto.AdAppendReqDto;
import com.example.egobook_be.domain.ads.dto.AdAppendResDto;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Ads API", description = "광고 할당, 시청 이벤트 처리 및 보상 관리 도메인")
public interface AdsControllerDocs {
    @Operation(summary = "광고 등록 API (Admin)",
            description = """
                    관리자가 새로운 광고를 등록합니다. (Role: ADMIN)
                    
                    ### [Request Body]
                    **(1) 영상 파일**
                        - ```videoFile``` : (필수) (MP4, MOV)
                    **(2) 광고주 정보**
                        - ```advertiserName```: 광고주 이름 (필수)
                        - ```advertiserEmail```: 광고주 이메일 (필수)
                    **(3) 광고 노출 정보**
                        - ```title```: 광고 제목 (필수)
                        - ```description```: 광고 상세 설명
                        - ```ctaText```: 버튼 텍스트 (Call To Action)
                        - ```landingUrl```: 클릭 시 이동할 랜딩 URL (필수)
                    **(4) 예산 및 보상 설정**
                        - ```totalBudget```: 총 예산 (필수)
                        - ```costPerView```: 시청 단가. 1회 시청 당 광고주 차감 비용 (필수)
                        - ```rewardInk```: 광고 시청 시 사용자가 받을 잉크 보상 값 (필수)
                        - ```rewardGrantSec```: 보상 지급 기준 시청 시간(초). 미입력 시 기본값 15초
                    **(5) 스케줄링**
                        - ```startAt```: 광고 시작 일시 (yyyy-MM-dd'T'HH:mm:ss)
                        - ```endAt```: 광고 종료 일시 (yyyy-MM-dd'T'HH:mm:ss)
    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "광고 생성 성공",
                    content = @Content(schema = @Schema(implementation = AdAppendResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 광고 설정 값 (Validation Error)"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (Not Admin)")
    })
    @PostMapping(value = "/ads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<GlobalResponse<AdAppendResDto>> appendAds(@ModelAttribute AdAppendReqDto request);

//    @Operation(summary = "광고 할당 요청 API",
//            description = """
//                    사용자가 시청할 광고를 할당해줍니다.
//                    """)
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "광고 할당 성공",
//                    content = @Content(schema = @Schema(implementation = AdAllocationResDto.class))),
//            @ApiResponse(responseCode = "404", description = "할당 가능한 광고 재고(Inventory) 없음"),
//            @ApiResponse(responseCode = "429", description = "요청 한도 초과 (Rate Limit Exceeded)")
//    })
//    ResponseEntity<AdAllocationResDto> allocateAd(
//            @Parameter(hidden = true) Object userPrincipal // Security Context에서 주입
//    );
//
//    @Operation(summary = "광고 시청 시작 이벤트",
//            description = "사용자가 광고 시청을 시작했음을 알립니다. 시청 시간 측정을 위한 타임스탬프가 기록됩니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "이벤트 기록 성공"),
//            @ApiResponse(responseCode = "400", description = "유효하지 않은 광고 ID"),
//            @ApiResponse(responseCode = "409", description = "이미 진행 중인 광고 세션 존재")
//    })
//    ResponseEntity<Void> startAdEvent(@RequestBody AdEventStartReqDto request);
//
//    @Operation(summary = "광고 시청 완료 및 보상 처리",
//            description = "기준 시간 이상 시청 시 호출됩니다. 유효성 검증 후 사용자에게 보상을 지급합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "시청 완료 및 보상 지급 성공",
//                    content = @Content(schema = @Schema(implementation = AdRewardResDto.class))),
//            @ApiResponse(responseCode = "400", description = "최소 시청 시간 미충족 또는 부정 요청"),
//            @ApiResponse(responseCode = "409", description = "이미 처리된 광고 (Idempotency Check)"),
//            @ApiResponse(responseCode = "500", description = "보상 트랜잭션 처리 실패")
//    })
//    ResponseEntity<AdRewardResDto> completeAdEvent(@RequestBody AdEventCompleteReqDto request);
//
//    @Operation(summary = "오늘의 광고 현황 및 기대 보상 조회",
//            description = "사용자가 오늘 시청 가능한 남은 광고 수와 획득 가능한 재화 정보를 반환합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "조회 성공",
//                    content = @Content(schema = @Schema(implementation = AdUserStatusResDto.class))),
//            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
//    })
//    ResponseEntity<AdUserStatusResDto> getAdInfo(
//            @Parameter(hidden = true) Object userPrincipal
//    );
}
