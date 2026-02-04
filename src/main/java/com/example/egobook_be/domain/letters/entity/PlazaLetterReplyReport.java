package com.example.egobook_be.domain.letters.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE) // 답장이 삭제되면 신고 내역도 자동 삭제
    private PlazaLetterReply reply;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id")
    @OnDelete(action = OnDeleteAction.CASCADE) // 편지가 삭제되면 신고 내역도 자동 삭제
    private PlazaLetter letter;

    @Column(nullable = false)
    private Long reporterId;

    private Long replierId; // 신고를 받은 편지를 작성한 작성자의 User PK

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
