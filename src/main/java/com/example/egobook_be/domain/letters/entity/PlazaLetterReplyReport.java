package com.example.egobook_be.domain.letters.entity;

import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import com.example.egobook_be.global.entity.BaseReportEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PlazaLetterReplyReport extends BaseReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PlazaLetterReply reply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PlazaLetter letter;

    @Column(nullable = false)
    private Long reporterId;

    private Long replierId;

    @Enumerated(EnumType.STRING)
    private ReplyReportReason reason;
}