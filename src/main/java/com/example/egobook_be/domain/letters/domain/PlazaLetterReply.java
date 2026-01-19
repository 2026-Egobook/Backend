package com.example.egobook_be.domain.letters.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "plaza_letter_replies")
public class PlazaLetterReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    @Column(nullable = false)
    private Long letterId;

    @Column(nullable = false)
    private Long replierId;

    @Column(nullable = false)
    private Long threadId;

    @Column(nullable = false, length = 350)
    private String text;

    @Column(nullable = false)
    private boolean isAiGenerated;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}

