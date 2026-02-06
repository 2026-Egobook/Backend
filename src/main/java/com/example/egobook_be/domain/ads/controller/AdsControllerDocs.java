package com.example.egobook_be.domain.ads.controller;

import com.example.egobook_be.domain.ads.dto.UserAdStatusResDto;
import com.example.egobook_be.domain.ads.enums.AdRewardType;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Ads API", description = "광고 시청 이벤트 처리 및 보상 관리 도메인")
@RequestMapping("/ads")
public interface AdsControllerDocs {
    @Operation(summary = "AdMob SSV Callback",
            description = """
                    사용자가 광고 보기 버튼을 눌러 시청을 완료하면, 구글 AdMob 서버가 이 API를 자동으로 호출하여 보상 지급을 요청합니다.
                    
                    **[ 특징 ]**
                    - 개발 단계에서는 '테스트 기기'로 등록된 폰에서 수행해야 합니다.
                    - `reward_item` (지급할 보상 아이템 타입)을 통해 어떤 상황의 광고(잉크 보상용 vs 주간 AI 보고서용)인지 구분합니다.
                    
                    **[ 프론트엔드 가이드 ]**
                    **(1) 보상 1:잉크 보상**
                    - UserId값으로 해당 사용자의 userId 값을 설정해주세요.
                    - 커스텀 데이터는 설정하지 않아주셔도 됩니다.
                    
                    **(2) 보상 2:주간 AI 리포트 확인**
                    - UserId값으로 해당 사용자의 userId 값을 설정해주세요.
                    - **커스텀 데이터에 보고 싶은 주간 AI 리포트의 PK값을 넣어주세요.**
                    
                    **[ 핵심 로직 ]**
                    1. **서명 검증:** ECDSA 서명을 통해 요청이 구글에서 온 것인지 확인합니다.
                    2. **중복 방지:** `transaction_id`를 통해 이미 지급된 보상인지 체크합니다.
                    3. **보상 지급:** 검증이 완료되면 사용자에게 보상을 지급합니다.

                    **[ 주의 ]**
                    - 이 API는 클라이언트(앱)가 직접 호출할 수 없으며, 호출해도 서명이 없으면 거부됩니다.

                    **[ 공식 문서 ]**
                    https://developers.google.com/admob/android/ssv?hl=ko
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공 (정상 지급 또는 이미 처리된 중복 건)"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락"),
            @ApiResponse(responseCode = "403", description = "서명 검증 실패 (위조된 요청)")
    })
    @GetMapping("/admob/callback")
    ResponseEntity<Void> callback(
            @Parameter(hidden = true) HttpServletRequest request, // 원본 쿼리 스트링 추출용 (서명 검증에 필수임)

            @Parameter(description = "AdMob에서 생성한 SSV 콜백 서명 값", required = true)
            @RequestParam("signature") String signature,

            @Parameter(description = "서명 검증에 사용할 키 ID", required = true)
            @RequestParam("key_id") String keyId,

            @Parameter(description = "광고 시청 고유 트랜잭션 ID (중복 방지 키)", required = true)
            @RequestParam("transaction_id") String transactionId,

            @Parameter(description = "사용자 식별자 (앱에서 설정한 값)", required = true)
            @RequestParam("user_id") String userId,

            @Parameter(description = "지급할 보상 아이템 타입")
            @RequestParam("reward_item") String rewardType,

            @Parameter(description = "광고 단위 ID (어떤 광고를 봤는지)")
            @RequestParam(value = "ad_unit", required = false) String adUnitId,

            @Parameter(description = "커스텀 데이터: 확인할 주간 보고서 ID")
            @RequestParam(value = "custom_data", required = false) String weeklyCounselId
    );



    @Operation(summary = "오늘의 광고 현황 및 기대 보상 조회",
            description = "사용자가 오늘 시청 가능한 남은 광고 수와 획득 가능한 재화 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserAdStatusResDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/info")
    ResponseEntity<GlobalResponse<UserAdStatusResDto>> getUserAdStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    );
}
