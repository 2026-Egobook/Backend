package com.example.egobook_be.domain.letters.entity;

import com.example.egobook_be.domain.letters.enums.PlazaLetterColor;
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

    @Column
    private Long senderId;

    @Column
    private Long receiverId; // RANDOM 매칭 전이면 null 가능


    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PlazaLetterColor backgroundColor = PlazaLetterColor.WHITE;

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

    @Column
    private OffsetDateTime arrivedAt;

    @Column
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

    public void markAiReplied(OffsetDateTime repliedAt) {
        this.status = PlazaLetterStatus.AI_REPLIED;
        this.repliedAt = repliedAt; // repliedAt 필드를 그대로 사용 (AI 답장)
    }

    public void assignToReceiver(Long receiverId, OffsetDateTime arrivedAt, OffsetDateTime replyDeadlineAt) {
        this.receiverId = receiverId;
        this.arrivedAt = arrivedAt;
        this.replyDeadlineAt = replyDeadlineAt;
        this.status = PlazaLetterStatus.ARRIVED;
    }

    public void assignReceiver(
            Long receiverId,
            OffsetDateTime arrivedAt,
            OffsetDateTime replyDeadlineAt
    ) {
        this.receiverId = receiverId;
        this.arrivedAt = arrivedAt;
        this.replyDeadlineAt = replyDeadlineAt;
        this.status = PlazaLetterStatus.ARRIVED;
    }


}
