package com.example.egobook_be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", length = 12)
    private String accountCode;

    @Column(nullable = false)
    private String role;

    @Column(length = 20)
    private String nickname;

    @Column(nullable = false)
    private String status;

    private String email;

    @Column(name = "streak_count", nullable = false)
    private Integer streakCount = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "purge_at")
    private LocalDateTime purgeAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "daily_praise")
    private Boolean dailyPraise;

    @Column(name = "weekly_report_style", length = 255)
    private String weeklyReportStyle;

    @Column(nullable = false)
    private Integer ink = 0;

    public void addInk(int amount) {
        this.ink += amount;
    }
}