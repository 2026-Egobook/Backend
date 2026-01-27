package com.example.egobook_be.domain.ego_room.entity;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.ego_room.enums.PraiseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPraise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    private boolean isRead;

    @Enumerated(EnumType.STRING)
    private PraiseStatus status;

    private LocalDateTime createdAt;

    @Builder
    public DailyPraise(String content, Diary diary, PraiseStatus status) {
        this.content = content;
        this.diary = diary;
        this.status = status;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}