package com.example.egobook_be.domain.question.entity;

import com.example.egobook_be.domain.question.enums.QuestionReportReason;
import com.example.egobook_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "answer_report",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "answer_id"})
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerReport {

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
    private QuestionReportReason reason;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}