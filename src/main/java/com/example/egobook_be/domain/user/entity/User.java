package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import com.example.egobook_be.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
// Private로 Access를 막아둠으로써, 외부 코드에서 new User(...)로 생성하는 것을 금지시킨다.
@AllArgsConstructor(access = AccessLevel.PRIVATE)
// Protected로 Acess를 설정함으로써, JPA/Hibernate는 접근 가능하지만 개발자가 직접 호출 못하게 설정한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "User")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 AUTO_INCREMENT와 같음
    private Long id;

    // 사용자에게 노출되는 고유 식별 코드 (예: EG7X9A21)
    @Column(name = "account_code", unique = true, nullable = false, length = 12)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private RoleType role = RoleType.ROLE_USER;

    @Column(length = 20) // 닉네임은 최대 8글자
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 255)
    private String email;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 1; // 사용자 레벨 기본값 1

    @Column(name = "purge_at")
    private LocalDateTime purgeAt; // 완전 삭제 예정 시각

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 요청 시각

    @Setter
    @Column(name = "daily_praise")
    @Builder.Default
    private Boolean dailyPraise = true; // AI 칭찬서 수신 여부 (기본값 true)

    @Setter
    @Column(name = "weekly_analysis_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean weeklyAnalysisEnabled = true;

    @Column(name = "ink")
    @Builder.Default
    private Integer ink = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true; // 알림 설정 (기본값 true)

    @Column(nullable = false)
    @Builder.Default
    private boolean isFirstAttendanceToday = true; // 오늘 첫 접속 상태인지 여부

    @Column(length = 500)
    private String fcmToken;

    // 편지 전송 조건 저장
    @Column(name = "letter_receive_blocked_until")
    private LocalDateTime letterReceiveBlockedUntil;

    // ========= 연관관계 매핑 ========= //

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Ability ability;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CounselTone counselingTone = CounselTone.SOFT;

    public void updateCounselingTone(CounselTone toneStyle) {
        this.counselingTone = toneStyle;
    }
    /*
     * 사용자가 보유한 아이템 리스트 (양방향 매핑)
     * User가 삭제되면 보유 목록도 같이 삭제되도록 CascadeType.ALL 설정
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserItem> userItems = new ArrayList<>();

    // ========= Entity 비즈니스 메서드 ========= //

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 사용자가 login했을 때 User Entity 스스로가 자신의 상태를 최신으로 갱신하는 함수
     * - 함수 동작
     *      1. 접속 시간 갱신
     *      2. 상태 변경 - 현재 상태가 DORMANT(휴면)일 경우, 활동 중(ACTIVE) 상태로 변경한다.
     */
    public void login() {
        this.lastLoginAt = LocalDateTime.now();
        if (this.status == UserStatus.DORMANT) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * 사용자 닉네임을 업데이트 하는 Entity 비즈니스 메서드입니다.
     * @param newNickname : 사용자가 업데이트 하고자 하는 새로운 닉네임
     */
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateEmail(String newEmail) {this.email = newEmail;}
    /**
     * 사용자가 탈퇴를 수행했을 때, 바로 삭제하지 않고 실제 삭제 예정 날짜를 설정하는 함수입니다.
     * (1) status -> WITHDRAW_PENDING
     * (2) deletedAt(삭제 요청 시각) 최신화
     * (3) purgeAt(완전 삭제 예정 시각) 최신화
     * (4) dailPraise (AI 칭찬서 수신 여부) false
     * (5) notificationEnabled (알림 설정) false
     * @param purgeDurationInMs : 완전 삭제까지의 기간(일주일)
     */
    public void withdrawUser(Long purgeDurationInMs) {
        this.status = UserStatus.WITHDRAW_PENDING;
        this.deletedAt = LocalDateTime.now();
        this.purgeAt = this.deletedAt.plus(purgeDurationInMs, ChronoUnit.MILLIS);
        this.dailyPraise = false;
        this.notificationEnabled = false;
    }

    public void cancelWithDrawUser(){
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
        this.purgeAt = null;
        this.dailyPraise = true;
        this.notificationEnabled = true;
    }

    public void addInk(int amount) {
        this.ink += amount;
    }

    public void useInk(int price){
        if (price <= 0) return;

        if (this.ink == null || this.ink < price) {
            throw new CustomException(LettersErrorCode.INSUFFICIENT_INK);
        }

        this.ink -= price;
    }

    public void updateNotificationEnabled() {
        this.notificationEnabled = !this.notificationEnabled;
    }

    public void updateDailyPraiseEnabled(boolean enabled) {
        this.dailyPraise = enabled;
    }

    public void updateWeeklyAnalysisEnabled(boolean enabled) {
        this.weeklyAnalysisEnabled = enabled;
    }

    /**
     * 출석 여부를 확인한 후, 출석을 하지 않았다면 출석 보상 부여 및 상태를 변경하는 함수
     * @param rewardAmount : 보상을 받을 잉크량
     * return : 지급된 보상 잉크량
     */
    public int checkFirstAttendanceTodayAndGetReward(int rewardAmount){
        // 1. 출석 여부 확인 - 이미 출석했다면 보상 0
        if(!this.isFirstAttendanceToday()){
            return 0;
        }

        // 2. 오늘 첫 접속 상태라면, 스스로 상태 변경 후 보상 지급
        this.isFirstAttendanceToday = false;
        this.addInk(rewardAmount);
        return rewardAmount;
    }



    public void blockLetterReceiveUntil(LocalDateTime until) {
        this.letterReceiveBlockedUntil = until;
    }

    public boolean canReceiveLetterAt(LocalDateTime now) {
        return letterReceiveBlockedUntil == null || !now.isBefore(letterReceiveBlockedUntil);
    }

    public void levelUp(){
        this.level += 1;
    }


    // 해당 시간까지 수신 차단된 상태인지 확인하는 메서드도 필요할 수 있음.
    public boolean canReceiveLetters() {
        return letterReceiveBlockedUntil == null || LocalDateTime.now().isAfter(letterReceiveBlockedUntil);
    }

    // getter, setter
    public LocalDateTime getLetterReceiveBlockedUntil() {
        return letterReceiveBlockedUntil;
    }

}