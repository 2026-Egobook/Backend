package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AnswerEmpathyResDto;
import com.example.egobook_be.domain.question.entity.AnswerEmpathy;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.AnswerEmpathyRepository;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerEmpathyService {

    private final AnswerEmpathyRepository answerEmpathyRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;

    @Transactional
    public AnswerEmpathyResDto toggleEmpathy(Long userId, Long answerId) {
        log.info("[AnswerEmpathyService] toggleEmpathy Start - userId: {}, answerId: {}", userId, answerId);
        QuestionAnswer answer = questionAnswerRepository.findByIdForUpdate(answerId)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND));

        Optional<AnswerEmpathy> empathy = answerEmpathyRepository.findByAnswer_IdAndUser_Id(answerId, userId);

        boolean empathized;
        if (empathy.isPresent()) {
            answerEmpathyRepository.delete(empathy.get());
            empathized = false;
        } else {
            if (answer.getVisibility() != AnswerVisibility.PUBLIC) {
                throw new CustomException(QuestionErrorCode.EMPATHY_NOT_ALLOWED);
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
            answerEmpathyRepository.save(
                    AnswerEmpathy.builder()
                            .answer(answer)
                            .user(user)
                            .build()
            );
            empathized = true;
        }
        answerEmpathyRepository.flush();

        long empathyCount = answerEmpathyRepository.countByAnswer_Id(answerId);
        log.info("[AnswerEmpathyService] toggleEmpathy End - userId: {}, answerId: {}, empathized: {}", userId, answerId, empathized);
        return AnswerEmpathyResDto.builder()
                .answerId(answerId)
                .empathized(empathized)
                .empathyCount(empathyCount)
                .build();
    }
}
