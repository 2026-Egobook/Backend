package com.example.egobook_be.plaza.letters.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "plaza_letters")
public class PlazaLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long letterId;

    // "내가 답장해야 할 편지" 기준: receiverId = 내 userId
    @Column(nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlazaLetterStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlazaLetterMode mode;

    @Column(nullable = false, length = 30)
    private String fromLabel; // "익명" or 친구 닉네임

    @Column(nullable = false, length = 400)
    private String content;

    @Column(nullable = false)
    private OffsetDateTime arrivedAt;

    @Column(nullable = false)
    private OffsetDateTime replyDeadlineAt;

    private OffsetDateTime repliedAt;

    public void markReplied(OffsetDateTime repliedAt) {
        this.status = PlazaLetterStatus.REPLIED;
        this.repliedAt = repliedAt;
    }

    public void markDeferred() {
        this.status = PlazaLetterStatus.DEFERRED;
    }

    @Column
    private OffsetDateTime gaveUpAt;

    public void markGaveUp(OffsetDateTime gaveUpAt) {
        this.status = PlazaLetterStatus.GAVE_UP;
        this.gaveUpAt = gaveUpAt;
    }

}
