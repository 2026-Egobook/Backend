package com.example.egobook_be.domain.restriction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// 내 제재 상태 조회 응답 DTO
@Builder
public record MyRestrictionResDto(

        @Schema(description = "편지 기능 제재 상태")
        RestrictionInfoResDto letter,

        @Schema(description = "오늘의 질문 답변 기능 제재 상태")
        RestrictionInfoResDto questionAnswer
) {}
