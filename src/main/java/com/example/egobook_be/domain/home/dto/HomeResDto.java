package com.example.egobook_be.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record HomeResDto(
        @Schema(description = "사용자 닉네임", example = "에고북1234")
        String nickname,
        @Schema(description = "사용자 레벨", example = "1")
        Integer level,
        @Schema(description = "사용자 보유 잉크", example = "100")
        Integer ink,
        @Schema(description = "사용자가 아직 읽지 않은 알림 유무", example = "true")
        boolean hasUnreadNotifications,
        @Schema(description = "사용자가 아직 열지 않은 오늘의 심리 지식 유무", example = "true")
        boolean hasUnopenedPsychology,
        @Schema(description = "오늘 최초 출석인지 여부", example = "true")
        boolean isFirstAttendanceToday,
        @Schema(description = "최초 출석 보상 잉크 값", example = " 3")
        Integer attendanceRewardInk
) {
}
