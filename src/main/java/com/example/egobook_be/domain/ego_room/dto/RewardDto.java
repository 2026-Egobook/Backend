package com.example.egobook_be.domain.ego_room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RewardDto(
        @Schema(description = "리워드 종류", example = "SELF_ESTEEM")
        String kind,
        @Schema(description = "리워드 양", example = "1")
        int amount,
        @Schema(description = "토스트 메시지", example = "칭찬서가 도착하여 자존감이 한 칸 상승했어요")
        String toastMessage
) {
}