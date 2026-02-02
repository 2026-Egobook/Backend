package com.example.egobook_be.domain.home.dto;

import com.example.egobook_be.domain.user.entity.AbilityStat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
public record HomeAbilityResDto(
        @Schema(description = "공감성 능력치 정보")
        AbilityInfo empathy,

        @Schema(description = "자존감 능력치 정보")
        AbilityInfo selfEsteem,

        @Schema(description = "성실함 능력치 정보")
        AbilityInfo diligence,

        @Schema(description = "긍정사고 능력치 정보")
        AbilityInfo positiveThinking,

        @Schema(description = "감정조절 능력치 정보")
        AbilityInfo emotionRegulation
) {
    @Getter
    @Builder
    public static class AbilityInfo{
        private Integer level;
        private Integer score;
        private String color;

    }
}
