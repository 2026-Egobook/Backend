package com.example.egobook_be.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PurchaseItemReqDto(
        @Schema(description = "구매할 아이템의 PK", example = "1")
        @NotNull
        Long itemId
) {
}
