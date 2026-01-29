package com.example.egobook_be.domain.home.entity;

import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mission")
public class Mission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- [일일 미션 상태] ---

    @Column(name = "daily_mission_success", nullable = false)
    @Builder.Default
    private boolean dailyMissionSuccess = false; // 하루 미션 최종 성공 여부

    @Column(name = "daily_diary_written", nullable = false)
    @Builder.Default
    private boolean dailyDiaryWritten = false; // 일기 쓰기 완료 여부

    @Column(name = "daily_letter_written", nullable = false)
    @Builder.Default
    private boolean dailyLetterWritten = false; // 편지 쓰기 완료 여부

    @Column(name = "daily_question_answered", nullable = false)
    @Builder.Default
    private boolean dailyQuestionAnswered = false; // 오늘의 질문 답변 완료 여부

    // --- [주간/연속 미션 상태] ---

    @Column(name = "consecutive_weeks", nullable = false)
    @Builder.Default
    private Integer consecutiveWeeks = 1; // 연속 진행 주차

    /**
     * 요일별 미션 수행 상태 문자열
     * format: "월화수목금토일" (0: 미수행, 1: 수행)
     * example: "1010000" (월, 수 완료)
     */
    @Column(name = "weekly_mission_status", nullable = false, length = 7)
    @Builder.Default
    private String weeklyMissionStatus = "0000000";

    // --- [연관 관계] ---

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ==================================================================
    // [Business Logic] 편의 메서드
    // ==================================================================

    /**
     * 1. 주간 미션 상태를 초기화 하는 함수 (스케줄러로 매주 월요일 자정 호출)
     * weekly_mission_status를 "0000000"으로 리셋
     */
    public void resetWeeklyMissionStatus() {
        this.weeklyMissionStatus = "0000000";
    }

    /**
     * 2. 일일 미션 상태를 초기화 하는 함수 (스케줄러로 매일 자정 호출)
     */
    public void resetDailyMissionStatus() {
        this.dailyMissionSuccess = false;
        this.dailyDiaryWritten = false;
        this.dailyLetterWritten = false;
        this.dailyQuestionAnswered = false;
    }

    /**
     * 특정 요일 미션 완료 처리 함수
     * @param dayOfWeek : Java Time DayOfWeek (MONDAY ~ SUNDAY)
     */
    public void completeDayMission(DayOfWeek dayOfWeek) {
        int index = dayOfWeek.getValue() - 1; // 0(월) ~ 6(일)
        char[] chars = this.weeklyMissionStatus.toCharArray();
        chars[index] = '1';
        this.weeklyMissionStatus = new String(chars);
    }

    // ==================================================================
    // [Getter Helpers] 요일별 상태 확인 함수
    // DTO 변환 시 mission.isMondayCompleted() 형태로 사용 가능
    // ==================================================================

    public boolean isMondayCompleted() {
        return checkStatusAtIndex(0);
    }

    public boolean isTuesdayCompleted() {
        return checkStatusAtIndex(1);
    }

    public boolean isWednesdayCompleted() {
        return checkStatusAtIndex(2);
    }

    public boolean isThursdayCompleted() {
        return checkStatusAtIndex(3);
    }

    public boolean isFridayCompleted() {
        return checkStatusAtIndex(4);
    }

    public boolean isSaturdayCompleted() {
        return checkStatusAtIndex(5);
    }

    public boolean isSundayCompleted() {
        return checkStatusAtIndex(6);
    }

    /**
     * 내부적으로 사용하는 인덱스 체크 헬퍼
     * '1'이면 true, '0'이면 false 반환
     */
    private boolean checkStatusAtIndex(int index) {
        if (this.weeklyMissionStatus == null || this.weeklyMissionStatus.length() != 7) {
            return false; // 방어 로직
        }
        return this.weeklyMissionStatus.charAt(index) == '1';
    }

    /**
     * 일일 미션 상태 업데이트 메서드
     */
    public void updateDailyStatus(boolean diary, boolean letter, boolean question) {
        this.dailyDiaryWritten = diary;
        this.dailyLetterWritten = letter;
        this.dailyQuestionAnswered = question;

        // 3개 중 하나만 true여도 DailyMission 성공
        this.dailyMissionSuccess = diary || letter || question;
    }
}