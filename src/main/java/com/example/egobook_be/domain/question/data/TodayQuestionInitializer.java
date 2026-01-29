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

    @PostConstruct
    public void initQuestions() {

        if (todayQuestionRepository.count() > 0) {
            return;
        }

        //26.01.27 부터 1번 질문 시작
        LocalDate startDate = LocalDate.of(2026, 1, 27);

        List<TodayQuestion> questions = new ArrayList<>();

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