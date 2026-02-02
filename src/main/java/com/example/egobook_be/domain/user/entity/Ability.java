package com.example.egobook_be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "ability")
public class Ability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // 1. 공감성 (Empathy)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "level", column = @Column(name = "empathy_level")),
            @AttributeOverride(name = "score", column = @Column(name = "empathy_score"))
    })
    @Builder.Default
    private AbilityStat empathy = new AbilityStat();

    // 2. 자존감 (SelfEsteem)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "level", column = @Column(name = "self_esteem_level")),
            @AttributeOverride(name = "score", column = @Column(name = "self_esteem_score"))
    })
    @Builder.Default
    private AbilityStat selfEsteem = new AbilityStat();

    // 3. 감정 조절 (EmotionRegulation)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "level", column = @Column(name = "emotion_regulation_level")),
            @AttributeOverride(name = "score", column = @Column(name = "emotion_regulation_score"))
    })
    @Builder.Default
    private AbilityStat emotionRegulation = new AbilityStat();

    // 4. 긍정적 사고 (PositiveThinking)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "level", column = @Column(name = "positive_thinking_level")),
            @AttributeOverride(name = "score", column = @Column(name = "positive_thinking_score"))
    })
    @Builder.Default
    private AbilityStat positiveThinking = new AbilityStat();

    // 5. 성실성 (Diligence)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "level", column = @Column(name = "diligence_level")),
            @AttributeOverride(name = "score", column = @Column(name = "diligence_score"))
    })
    @Builder.Default
    private AbilityStat diligence = new AbilityStat();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // =======================================================
    // [Business Logic] 외부에서 호출하는 편의 메서드
    // =======================================================

    public Integer addEmotionRegulation(int amount) {
        return this.emotionRegulation.addScore(amount);
    }

    public Integer addPositiveThinking(int amount) {
        return this.positiveThinking.addScore(amount);
    }

    public Integer addEmpathy(int amount) {
        return this.empathy.addScore(amount);
    }

    public Integer addDiligence(int amount) {
        return this.diligence.addScore(amount);
    }

    public Integer addSelfEsteem(int amount) {
        return this.selfEsteem.addScore(amount);
    }
    public void addSelfEsteem(int value) {
        this.selfEsteem += value;
    }


}
