package com.example.egobook_be.domain.ads.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TestAdRewardReqDto(
        @Schema(description = "보상 타입 (INK 또는 WEEK_COUNSEL)", example = "INK")
        @NotBlank
        String rewardType,

        @Schema(description = "타겟 ID (INK일 경우 null, 주간 리포트일 경우 해당 리포트 ID)", example = "105")
        Long targetId, // 선택적 값이므로 Long 객체 사용 (null 허용)

        @Schema(description = "광고 단위 ID (로깅용, 선택 사항)", example = "ca-app-pub-test/12345")
        String adUnitId
) {
}
