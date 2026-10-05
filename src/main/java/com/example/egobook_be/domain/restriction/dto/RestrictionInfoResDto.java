package com.example.egobook_be.domain.restriction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

// 단일 도메인 제재 상태 응답 DTO
@Builder
public record RestrictionInfoResDto(

        @Schema(description = "현재 제재 중인지 여부", example = "true")
        boolean restricted,

        @Schema(description = "팝업에 그대로 표시할 제재 사유 (제재 중이 아니면 null)",
                example = "커뮤니티 이용 규칙 위반 (반복된 신고 접수)", nullable = true)
        String reason,

        @Schema(description = "제재 종료 시각, 한국 시간 (이 시각부터 다시 이용 가능, 제재 중이 아니면 null)",
                example = "2026-10-13T04:26:58", nullable = true)
        LocalDateTime restrictedUntil
) {}
