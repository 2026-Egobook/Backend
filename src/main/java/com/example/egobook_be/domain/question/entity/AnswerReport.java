package com.example.egobook_be.domain.question.entity;

import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseReportEntity;
import com.example.egobook_be.global.enums.ReportReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "answer_report",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "answer_id"})
        }
)
@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@SuperBuilder
public class AnswerReport extends BaseReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private QuestionAnswer answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

}