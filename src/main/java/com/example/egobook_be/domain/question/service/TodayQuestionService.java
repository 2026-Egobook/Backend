package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AnswerCreateReqDto;
import com.example.egobook_be.domain.question.dto.TodayQuestionResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TodayQuestionService {

    private final TodayQuestionRepository todayQuestionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;

    /** 오늘의 질문 조회 **/
    @Transactional(readOnly = true)
    public TodayQuestionResDto getTodayQuestion(Long userId) {

        TodayQuestion question = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new IllegalStateException("오늘의 질문이 존재하지 않습니다.")
                );

        boolean answered = false;

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow();

            answered = questionAnswerRepository
                    .findByUserAndQuestion(user, question)
                    .isPresent();
        }

        return TodayQuestionResDto.builder()
                .questionId(question.getId())
                .content(question.getContent())
                .date(question.getQuestionDate())
                .answered(answered)
                .build();
    }

    /** 답변 작성 **/
    @Transactional
    public void createAnswer(Long userId, AnswerCreateReqDto reqDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        if (questionAnswerRepository.existsByUserAndQuestion(user, todayQuestion)) {
            throw new CustomException(QuestionErrorCode.ALREADY_ANSWERED_TODAY);
        }

        questionAnswerRepository.save(
                QuestionAnswer.builder()
                        .user(user)
                        .question(todayQuestion)
                        .content(reqDto.content())
                        .visibility(reqDto.visibility())
                        .build()
        );
    }
}
