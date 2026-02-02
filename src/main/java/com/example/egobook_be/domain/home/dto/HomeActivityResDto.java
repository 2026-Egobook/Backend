package com.example.egobook_be.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record HomeActivityResDto(
        @Schema(description = "오늘의 하루 미션 최종 성공 여부 (3개 중 하나만 해도 true)", example = "false")
        boolean isDailyMissionSuccess,

        @Schema(description = "오늘 감정일기 작성 수행 여부", example = "true")
        boolean hasWrittenDiary,

        @Schema(description = "편지 쓰기 수행 여부", example = "false")
        boolean hasWrittenLetter,

        @Schema(description = "오늘의 질문 답변 수행 여부", example = "true")
        boolean hasAnsweredQuestion,

        @Schema(description = "연속 진행 주차 (N주차)", example = "3")
        Integer consecutiveWeeks,

        @Schema(description = "이번 주(월~일) 미션 수행 여부 리스트 (순서대로 월,화,수,목,금,토,일)", example = "[true, true, false, false, false, false, false]")
        List<Boolean> weeklyMissionStatus
) {
}
