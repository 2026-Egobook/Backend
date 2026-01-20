package com.example.egobook_be.domain.psychology.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "psychology_knowledge") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) public class PsychologyKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}