package com.example.egobook_be.domain.restriction.dto;

import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

// 사용자 제재 기록 목록 조회 응답 DTO
@Builder
public record RestrictionItemResDto(

        @Schema(description = "Restriction 도메인 PK")
        Long restrictionId,

        @Schema(description = "제재 도메인 타입 (LETTER | QUESTION_ANSWER)")
        RestrictionDomainType domainType,

        @Schema(description = "제재 사유")
        String reason,

        @Schema(description = "제재 사유 상세 설명")
        String description,

        @Schema(description = "제재 상태 (ACTIVE | CANCELED)")
        RestrictionStatus restrictionStatus,

        @Schema(description = "제재 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "제재 종료 예정 시각")
        LocalDateTime restrictionUntil
) {}
