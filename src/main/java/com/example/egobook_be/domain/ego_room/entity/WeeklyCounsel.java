package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyCounsel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String praisePoints;

    @Column(columnDefinition = "TEXT")
    private String improvementPoints;

    @Column(columnDefinition = "TEXT")
    private String managementAdvice;

    @Column(columnDefinition = "TEXT")
    private String supportMessage;

    private boolean isRead;

    private LocalDateTime createdAt;

    @Builder
    public WeeklyCounsel(User user, LocalDate startDate, LocalDate endDate, String summary, String praisePoints, String improvementPoints, String managementAdvice, String supportMessage) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.summary = summary;
        this.praisePoints = praisePoints;
        this.improvementPoints = improvementPoints;
        this.managementAdvice = managementAdvice;
        this.supportMessage = supportMessage;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void unlock() {
        this.isRead = true;
    }
}