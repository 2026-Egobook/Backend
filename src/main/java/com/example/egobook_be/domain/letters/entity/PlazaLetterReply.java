package com.example.egobook_be.domain.letters.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // 편지가 삭제되면 답장도 자동 삭제
    private PlazaLetter letter;

    @Column
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

