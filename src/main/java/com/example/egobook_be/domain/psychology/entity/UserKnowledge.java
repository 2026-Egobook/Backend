package com.example.egobook_be.domain.psychology.entity;


import com.example.egobook_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_knowledge")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychology_knowledge_id", nullable = false)
    private PsychologyKnowledge psychologyKnowledge;

    @Column(name = "saved_at", nullable = true)
    private LocalDateTime savedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean isBookmarked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 데이터가 처음 저장될 때 현재 시간을 자동으로 넣어주는 마법의 메서드야!
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateBookmarkStatus(boolean status) {
        this.isBookmarked = status;
        if (status) {
            this.savedAt = LocalDateTime.now();
        } else {
            this.savedAt = null;
        }
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isBookmarked = false;
        this.savedAt = null;
    }


    public UserKnowledge(User user, PsychologyKnowledge psychologyKnowledge) {
        this.user = user;
        this.psychologyKnowledge = psychologyKnowledge;
        this.savedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.isBookmarked = true;
        this.savedAt = LocalDateTime.now();
    }

}