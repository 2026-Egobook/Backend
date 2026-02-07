package com.example.egobook_be.domain.ads.dto;

import lombok.Builder;

@Builder
public record UserAdStatusResDto(
        int currentViewCount, // 오늘 본 횟수
        int maxLimit, // 최대 횟수 (10)
        boolean isAvailable, // 시청 가능 여부
        int rewardPerAd, // 1회당 보상량
        String message
) {
}
