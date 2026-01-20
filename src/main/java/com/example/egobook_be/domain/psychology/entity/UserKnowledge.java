package com.example.egobook_be.domain.psychology.entity;


import com.example.egobook_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_knowledge")
@Getter
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

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public UserKnowledge(User user, PsychologyKnowledge psychologyKnowledge) {
        this.user = user;
        this.psychologyKnowledge = psychologyKnowledge;
        this.savedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.savedAt = LocalDateTime.now();
    }
}