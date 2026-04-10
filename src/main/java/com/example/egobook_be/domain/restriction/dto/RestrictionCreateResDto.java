package com.example.egobook_be.domain.restriction.dto;

import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RestrictionCreateResDto(

        @Schema(description = "Restriction 도메인 PK")
        Long restrictionId,

        @Schema(description = "제재 상태 (ACTIVE | CANCELED)")
        RestrictionStatus restrictionStatus,

        @Schema(description = "제재 종료 예정 시간")
        LocalDateTime restrictionUntil
) {}
