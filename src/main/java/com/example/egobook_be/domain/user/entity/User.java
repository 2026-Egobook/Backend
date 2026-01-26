package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.user.enums.RoleType;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.enums.WeeklyReportStyle;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private Integer streakCount = 0; // 연속 출석 수 기본값 0

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "symbol_level", nullable = false)
    @Builder.Default
    private Integer level = 1; // 사용자 레벨 기본값 1

    @Column(name = "purge_at")
    private LocalDateTime purgeAt; // 완전 삭제 예정 시각

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 요청 시각

    @Column(name = "daily_praise")
    @Builder.Default
    private Boolean dailyPraise = true; // AI 칭찬서 수신 여부 (기본값 true)

    @Column(name = "weekly_report_style")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WeeklyReportStyle weeklyReportStyle = WeeklyReportStyle.SOFT; // 주간 AI 상담서 스타일 (다음주 상담 스타일. 기본은 "부드러움")

    @Column(name = "ink")
    @Builder.Default
    private Integer ink = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true; // 알림 설정 (기본값 true)

    // ========= 연관관계 매핑 ========= //

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Ability ability;


    /*
     * 사용자가 보유한 아이템 리스트 (양방향 매핑)
     * User가 삭제되면 보유 목록도 같이 삭제되도록 CascadeType.ALL 설정
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserItem> userItems = new ArrayList<>();

    // ========= Entity 비즈니스 메서드 ========= //
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
     * @param purgeDurationInMs : 완전 삭제까지의 기간(일주일)
     */
    public void deleteUser(Long purgeDurationInMs) {
        this.status = UserStatus.DELETED_PENDING;
        this.deletedAt = LocalDateTime.now();
        this.purgeAt = this.deletedAt.plus(purgeDurationInMs, ChronoUnit.MILLIS);
    }

    public void addInk(int amount) {
        this.ink += amount;
    }

    public void purchaseItem(int price){
        this.ink -= price;
    }

    public void updateNotificationEnabled() {
        this.notificationEnabled = !this.notificationEnabled;
    }
}