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
public class PlazaLetterReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // 편지가 삭제되면 신고 내역도 자동 삭제
    private PlazaLetter letter;


    @Column(name = "reporter_id", nullable = false)
    private Long reporterId; // 신고한 사람(= 편지 받은 사람)

    private Long senderId;   // 신고당한 편지 작성자(익명 처리/정책에 따라 null 가능)

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
