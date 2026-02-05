package com.example.egobook_be.domain.ads.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Ads API", description = "광고 시청 이벤트 처리 및 보상 관리 도메인")
@RequestMapping("/ads")
public interface AdsControllerDocs {

    @GetMapping("/admob/callback")
    ResponseEntity<Void> callback();


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
