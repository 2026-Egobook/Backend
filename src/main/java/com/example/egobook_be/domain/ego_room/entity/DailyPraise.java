package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.ego_room.enums.PraiseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import com.example.egobook_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPraise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;


    private boolean isRead;


    @Column(name = "praise_date", nullable = false)
    private LocalDate praiseDate;


    private LocalDateTime createdAt;

    @Builder
    public DailyPraise(User user, String content,  LocalDate praiseDate) {
        this.user = user;
        this.content = content;
        this.praiseDate = praiseDate; // 서비스에서 넘겨주는 날짜
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}