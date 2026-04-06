package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "관리자 회원 활동 통계 조회 DTO")
@Builder
public record AdminUserStatsResDto(
        @Schema(description = "사용자 ID") Long userId,
        @Schema(description = "카테고리별 활동 횟수") ActivityCount activityCount,
        @Schema(description = "능력치별 레벨") AbilityLevel abilityLevel,
        @Schema(description = "편지 수신 차단 종료 일시 (null이면 차단 없음)") LocalDateTime letterReceiveBlockedUntil,
        @Schema(description = "알림 수신 여부 (true: 수신, false: 비수신)") boolean notificationEnabled,
        @Schema(description = "오늘 첫 출석 여부 (true: 첫 출석 안함, false: 첫 출석 함)") boolean isFirstAttendanceToday,
        @Schema(description = "주간 분석 보고서 수신 여부 (true: 수신, false: 비수신)") boolean weeklyAnalysisEnabled,
        @Schema(description = "주간 분석 말투 (SHARP | SOFT | OBJECTIVE)") CounselTone counselingTone
) {
    @Schema(description = "카테고리별 활동 횟수")
    @Builder
    public record ActivityCount(
            @Schema(description = "일기 작성 횟수") long diary,
            @Schema(description = "편지 작성 횟수") long letter,
            @Schema(description = "편지 답변 횟수") long letterReply,
            @Schema(description = "오늘의 질문 답변 횟수") long questionAnswer
    ) {}

    @Schema(description = "능력치별 레벨")
    @Builder
    public record AbilityLevel(
            @Schema(description = "공감성 레벨") int empathy,
            @Schema(description = "자존감 레벨") int selfEsteem,
            @Schema(description = "감정조절 레벨") int emotionRegulation,
            @Schema(description = "긍정적 사고 레벨") int positiveThinking,
            @Schema(description = "성실성 레벨") int diligence
    ) {}
}
