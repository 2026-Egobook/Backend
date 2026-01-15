package com.example.egobook_be.domain.user.entity;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.NEW;

    @Column(length = 255)
    private String email;

    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private Integer streakCount = 0; // 연속 출석 수 기본값 0

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "symbol_level", nullable = false)
    @Builder.Default
    private Integer symbolLevel = 1; // 레벨 기본값 1

    @Column(name = "purge_at")
    private LocalDateTime purgeAt; // 완전 삭제 예정 시각

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 요청 시각

    @Column(name = "daily_praise")
    private Boolean dailyPraise; // AI 칭찬서 수신 여부 (Null 허용이므로 Boolean Wrapper 사용)

    @Column(name = "weekly_report_style")
    private String weeklyReportStyle; // 주간 AI 상담서 스타일 (다음주 상담 스타일)

    // ========= 연관관계 매핑 ========= //




    // ========= Entity 비즈니스 메서드 ========= //
    /**
     * 사용자가 login했을 때 User Entity 스스로가 자신의 상태를 최신으로 갱신하는 함수
     * - 함수 동작
     *      1. 접속 시간 갱신
     *      2. 상태 변경 - 현재 상태가 NEW이거나 DORMANT(휴면)일 경우, 활동 중(ACTIVE) 상태로 변경한다.
     */
    public void login() {
        this.lastLoginAt = LocalDateTime.now();
        if (this.status == UserStatus.NEW || this.status == UserStatus.DORMANT) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void deleteUser(LocalDateTime purgeTime) {
        this.status = UserStatus.DELETED_PENDING;
        this.deletedAt = LocalDateTime.now();
        this.purgeAt = purgeTime;
    }


}
