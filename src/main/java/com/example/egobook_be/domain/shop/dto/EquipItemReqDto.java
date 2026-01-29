package com.example.egobook_be.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record EquipItemReqDto(
        @Schema(description = "대상 아이템의 PK", example = "101")
        @NotNull(message = "아이템 ID는 필수입니다.")
        Long itemId,

        @Schema(description = "착용 여부 설정 (true: 착용, false: 해제)", example = "true")
        @NotNull(message = "착용/해제 여부(isEquipped) 값은 필수입니다.")
        Boolean isEquipped
) {
}
