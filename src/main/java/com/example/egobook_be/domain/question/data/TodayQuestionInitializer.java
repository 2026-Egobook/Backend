package com.example.egobook_be.domain.question.data;

import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodayQuestionInitializer {

    private final TodayQuestionRepository todayQuestionRepository;

    /**
     * 서버 최초 실행 시
     * today_question 테이블이 비어 있으면 질문 30개 전부 삽입
     */
    @PostConstruct
    public void initQuestions() {

        if (todayQuestionRepository.count() > 0) {
            return; // 이미 데이터 있으면 아무 것도 안 함
        }

        List<TodayQuestion> questions = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 0; i < TodayQuestionData.QUESTIONS.size(); i++) {
            questions.add(
                    TodayQuestion.builder()
                            .content(TodayQuestionData.QUESTIONS.get(i))
                            .questionDate(startDate.plusDays(i))
                            .build()
            );
        }

        todayQuestionRepository.saveAll(questions);
    }
}