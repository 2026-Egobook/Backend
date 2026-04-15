package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import com.example.egobook_be.global.exception.CustomException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "User")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", unique = true, nullable = false, length = 12)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private RoleType role = RoleType.ROLE_USER;

    @Column(length = 20)
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
    private Integer level = 1;

    @Column(name = "purge_at")
    private LocalDateTime purgeAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Setter
    @Column(name = "daily_praise")
    @Builder.Default
    private Boolean dailyPraise = true;

    @Setter
    @Column(name = "weekly_analysis_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean weeklyAnalysisEnabled = true;

    @Column(name = "ink")
    @Builder.Default
    private Integer ink = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isFirstAttendanceToday = true;

    @Column(length = 500)
    private String fcmToken;

    @Column(name = "letter_receive_blocked_until")
    private LocalDateTime letterReceiveBlockedUntil;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Ability ability;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CounselTone counselingTone = CounselTone.SOFT;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserItem> userItems = new ArrayList<>();

    public void updateCounselingTone(CounselTone toneStyle) {
        this.counselingTone = toneStyle;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 사용자가 login 했을 때 User Entity 스스로 자신의 상태를 최신으로 갱신하는 함수
     * - 접속 시간 갱신
     * - 현재 상태가 DORMANT 이면 ACTIVE 상태로 변경
     */
    public void login() {
        this.lastLoginAt = LocalDateTime.now();
        if (this.status == UserStatus.DORMANT) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * 사용자 닉네임을 업데이트하는 Entity 비즈니스 메서드이다.
     * @param newNickname 새로운 닉네임
     */
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }

    /**
     * 사용자가 탈퇴를 수행했을 때 soft delete 정보를 설정한다.
     * @param purgeDurationInMs 완전 삭제까지의 유예 기간
     */
    public void withdrawUser(Long purgeDurationInMs) {
        this.status = UserStatus.WITHDRAW_PENDING;
        this.deletedAt = LocalDateTime.now();
        this.purgeAt = this.deletedAt.plus(purgeDurationInMs, ChronoUnit.MILLIS);
        this.dailyPraise = false;
        this.notificationEnabled = false;
    }

    public void cancelWithDrawUser() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
        this.purgeAt = null;
        this.dailyPraise = true;
        this.notificationEnabled = true;
    }

    // [AI-GEN] restrict user status
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    // [AI-GEN] restore user status
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void addInk(int amount) {
        this.ink += amount;
    }

    public void useInk(int price) {
        if (price <= 0) {
            return;
        }

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
     * 오늘 첫 출석 여부를 확인하고 보상을 지급한다.
     * @param rewardAmount 보상 잉크
     * @return 지급된 보상 잉크
     */
    public int checkFirstAttendanceTodayAndGetReward(int rewardAmount) {
        if (!this.isFirstAttendanceToday()) {
            return 0;
        }

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

    public void levelUp() {
        this.level += 1;
    }

    public boolean canReceiveLetters() {
        return letterReceiveBlockedUntil == null || LocalDateTime.now().isAfter(letterReceiveBlockedUntil);
    }

    public LocalDateTime getLetterReceiveBlockedUntil() {
        return letterReceiveBlockedUntil;
    }
}
