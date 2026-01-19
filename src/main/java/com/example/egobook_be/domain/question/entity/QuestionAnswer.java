package com.example.egobook_be.domain.question.entity;

import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "question_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_question",
                        columnNames = {"user_id", "question_id"}
                )
        }
)
public class QuestionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 질문 **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TodayQuestion question;

    /** 답변 작성자 **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerVisibility visibility;

    public void update(String content, AnswerVisibility visibility) {
        this.content = content;
        this.visibility = visibility;
    }
}
