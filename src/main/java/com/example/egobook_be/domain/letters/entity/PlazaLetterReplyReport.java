package com.example.egobook_be.domain.letters.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlazaLetterReplyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private PlazaLetterReply reply;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id")
    private PlazaLetter letter;

    @Column(nullable = false)
    private Long reporterId;

    private Long replierId;

    @Enumerated(EnumType.STRING)
    private ReplyReportReason reason;

    private String description;

    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    public enum ReportStatus {
        PENDING, RESOLVED
    }
}
