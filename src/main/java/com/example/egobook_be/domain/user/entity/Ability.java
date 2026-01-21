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

    Integer empathy;
    Integer selfEsteem;
    Integer emotionRegulation;
    Integer positiveThinking;
    Integer diligence;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public void addEmotionRegulation(int i) {
        this.emotionRegulation += i;
    }

    public void addPositiveThinking(int i) {
        this.positiveThinking += i;
    }
}
