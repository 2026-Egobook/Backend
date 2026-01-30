package com.example.egobook_be.domain.home.entity;

import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
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

    @Column(nullable = false)
    @Builder.Default
    private LocalDate lastWeeklyResetDate = LocalDate.now(); // 가장 최근에 "주간 초기화"를 수행한 날짜

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
     * 주간 미션 상태를 초기화 하는 함수 (스케줄러로 매주 월요일 자정 호출)
     * weekly_mission_status를 "0000000"으로 리셋
     */
    public void resetWeeklyMissionStatus() {
        this.weeklyMissionStatus = "0000000";
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

    /** 일일 일기 작성 미션 상태를 변경하는 함수 (처음 일일 미션을 수행한 경우 true 반환)*/
    public boolean updateDailyDiaryMissionStatus(boolean dailyDiaryWritten) {
        return updateDailyMissionStatus(dailyDiaryWritten, this.dailyLetterWritten, this.dailyQuestionAnswered);
    }

    /** 일일 편지 작성 미션 상태를 변경하는 함수 (처음 일일 미션을 수행한 경우 true 반환)*/
    public boolean updateDailyLetterMissionStatus(boolean dailyLetterWritten) {
        return updateDailyMissionStatus(this.dailyDiaryWritten, dailyLetterWritten, this.dailyQuestionAnswered);
    }

    /** 일일 질문 답변 작성 미션 상태를 변경하는 함수 (처음 일일 미션을 수행한 경우 true 반환)*/
    public boolean updateDailyQuestionMissionStatus(boolean dailyQuestionAnswered) {
        return updateDailyMissionStatus(this.dailyDiaryWritten, this.dailyLetterWritten, dailyQuestionAnswered);
    }

    /**
     * 일일 미션 상태 업데이트 메서드
     * - 매 미션을 수행할 때마다, 주간 미션이 업데이트되었는지 검사한 후 미션 상태를 변경한다.
     * - 처음 일일 미션을 수행한 경우 true를 반환하며, 처음 일일 미션을 수행한 경우가 아니면 false를 반환한다.
     */
    private boolean updateDailyMissionStatus(boolean diary, boolean letter, boolean question) {
        // 1. 매 미션 수행 시, 주간 미션이 업데이트되어있는지 상태를 검사한 후 미션 상태를 변경한다.
        this.checkAndResetWeekly(LocalDate.now());

        // 2. 미션 상태 변경
        this.dailyDiaryWritten = diary;
        this.dailyLetterWritten = letter;
        this.dailyQuestionAnswered = question;

        boolean prevDailyMissionStatus = this.dailyMissionSuccess;
        // 3개 중 하나만 true여도 DailyMission 성공
        this.dailyMissionSuccess = diary || letter || question;
        // 이번 함수 호출 때 일일 미션을 성공한 경우, weeklyMissionStatus 변환한 후 true 반환
        boolean tmp = !prevDailyMissionStatus && this.dailyMissionSuccess;
        if(tmp){
            // 1. 오늘이 무슨 요일인지 확인 (숫자값: 1=월요일 ~ 7=일요일)
            LocalDate todayDay = LocalDate.now();
            DayOfWeek dayOfWeek = todayDay.getDayOfWeek();
            int dayOfWeekIndex = dayOfWeek.getValue() - 1;
            // 1. 문자 배열로 변환
            char[] chars = this.weeklyMissionStatus.toCharArray();

            // 2. 배열 인덱스로 접근하여 값 변경
            chars[dayOfWeekIndex] = '1';

            // 3. 다시 문자열로 생성하여 할당
            this.weeklyMissionStatus = new String(chars);
        }
        return tmp;
    }

    /** 이번주 미션이 모두 완료되었는지 여부 확인하는 함수*/
    public boolean isWeeklyMissionCompleted(){
        return weeklyMissionStatus.equals("1111111");
    }


    /**
     * 주간 데이터 정합성 체크 및 초기화 메서드
     * (조회 시점에 호출)
     */
    public void checkAndResetWeekly(LocalDate today) {
        // 1. 오늘 날짜가 속한 주의 월요일 구하기
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        log.info("이번주 월요일: {}", thisMonday);
        // 2. 마지막 초기화 날짜가 이번 주 월요일보다 이전인 경우 -> 초기화 필요
        if (this.lastWeeklyResetDate.isBefore(thisMonday)) {
            // 2-1. 연속 성공 여부 계산 (지난주에 다 채웠는지 확인 로직 필요)
            calculateConsecutiveWeeks(thisMonday);
            // 2-2. 주간 상태판 리셋 ("0000000")
            this.resetWeeklyMissionStatus();
            // 2-3. 초기화 날짜 갱신
            this.lastWeeklyResetDate = thisMonday;
        }
    }

    private void calculateConsecutiveWeeks(LocalDate thisMonday) {
        // 1. 마지막 초기화 날짜 ~ 이번주 월요일 사이의 주 차이 계산 (시작 날짜, 종료 날짜)
        long weeksBetween = ChronoUnit.WEEKS.between(lastWeeklyResetDate, thisMonday);
        log.info("마지막 초기화 날짜 ~ 이번주 월요일 사이의 주 차이 : {}", weeksBetween);

        // 2주 이상 미션을 수행을 안했다면 1로 초기화
        if(weeksBetween > 1) {
            this.consecutiveWeeks = 1;
            return;
        }

        // 지난주 주간 미션 상태가 모두 1이면 consecutiveWeeks 값을 +1 한다
        if (this.weeklyMissionStatus.equals("1111111")) {
            this.consecutiveWeeks++;
        } else {
            // 지난주 주간 미션을 전부 수행하지 못했다면 1으로 초기화
            this.consecutiveWeeks = 1;
        }
    }


}