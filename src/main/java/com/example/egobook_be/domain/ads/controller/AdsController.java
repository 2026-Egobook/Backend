package com.example.egobook_be.domain.ads.controller;

import com.example.egobook_be.domain.ads.dto.TestAdRewardReqDto;
import com.example.egobook_be.domain.ads.dto.UserAdStatusResDto;
import com.example.egobook_be.domain.ads.service.AdsService;
import com.example.egobook_be.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdsController implements AdsControllerDocs{
    private final AdsService adsService;

    @Override
    public ResponseEntity<Void> callback(
            @Parameter(hidden = true) HttpServletRequest request, // 원본 쿼리 스트링 추출용 (서명 검증에 필수임)

            @Parameter(description = "AdMob에서 생성한 SSV 콜백 서명 값", required = true, example = "TEST_PASS")
            @RequestParam("signature") String signature,

            @Parameter(description = "서명 검증에 사용할 키 ID", required = true, example = "test_key_123")
            @RequestParam("key_id") String keyId,

            @Parameter(description = "광고 시청 고유 트랜잭션 ID (중복 방지 키)", required = true, example = "TX_TEST_001")
            @RequestParam("transaction_id") String transactionId,

            @Parameter(description = "사용자 식별자 (앱에서 설정한 값)", required = true, example = "1")
            @RequestParam("user_id") String userId,

            @Parameter(description = "지급할 보상 아이템 타입 (INK or WEEK_COUNSEL)", example = "INK")
            @RequestParam("reward_item") String rewardType,

            @Parameter(description = "광고 단위 ID (어떤 광고를 봤는지)", required = false, example = "ca-app-pub-3940256099942544/5224354917")
            @RequestParam(value = "ad_unit", required = false) String adUnitId,

            @Parameter(description = "커스텀 데이터: 확인할 주간 보고서 ID", required = false, example = "")
            @RequestParam(value = "custom_data", required = false) String weeklyCounselId
    ){
        /*
         * 1. 원본 쿼리 스트링 추출 (서명 검증의 핵심 재료)
         * - Spring이 파싱한 @RequestParam들은 순서가 보장되지 않으므로, 반드시 request.getQueryString()으로 원본을 가져와야 합니다.
         */
        String queryString = request.getQueryString();
        try {
            // 2. 서비스 레이어로 위임
            adsService.adMobCallbackInk(
                    queryString,
                    signature,
                    keyId,
                    transactionId,
                    userId,
                    weeklyCounselId,
                    rewardType,
                    adUnitId
            );

            // 3. 성공 시 무조건 200 OK
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            // 서명 검증 실패 시: 명확하게 403 Forbidden 반환하여 구글에게 거부 의사 표시
            log.error("[AdMob Callback] 서명 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            // 그 외 DB 오류 등: 500 에러를 뱉으면 구글이 나중에 재시도(Retry)를 합니다.
            log.error("[AdMob Callback] 내부 처리 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<GlobalResponse<Void>> grantTestAdReward(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "보상 요청 정보", required = true,
                    content = @Content(schema = @Schema(implementation = TestAdRewardReqDto.class))
            )
            @RequestBody @Valid TestAdRewardReqDto reqDto
    ){
        adsService.grantTestAdReward(userId, reqDto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobalResponse.success("광고 보상 수령 완료", null));
    }

    @Override
    public ResponseEntity<GlobalResponse<UserAdStatusResDto>> getUserAdStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "userAuthDto.userId") Long userId
    ){
        UserAdStatusResDto resDto = adsService.getUserAdStatus(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(GlobalResponse.success("해당 사용자의 광고들의 정보 조회 완료", resDto));
    }
}
