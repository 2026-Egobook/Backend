package com.example.egobook_be.domain.letters.entity;

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

    @Column(nullable = false)
    private Long threadId;

    @Column(nullable = false)
    private Long senderId;

    @Column
    private Long receiverId; // RANDOM 매칭 전이면 null 가능


    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(length = 20)
    private String backgroundColor;

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
