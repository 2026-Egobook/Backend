package com.example.egobook_be.domain.question.entity;

import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "today_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_today_question_date",
                        columnNames = "question_date"
                )
        }
)
public class TodayQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;
}
