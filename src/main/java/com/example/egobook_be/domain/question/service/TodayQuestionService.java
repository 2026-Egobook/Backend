package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.friend.entity.Friend;
import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.question.dto.*;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodayQuestionService {

    private final TodayQuestionRepository todayQuestionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

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

    /** 오늘의 질문 PUBLIC 답변 전체 조회 **/
    @Transactional(readOnly = true)
    public List<PublicAnswerResDto> getPublicAnswers() {

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        return questionAnswerRepository
                .findByQuestionAndVisibility(todayQuestion, AnswerVisibility.PUBLIC)
                .stream()
                .map(answer -> PublicAnswerResDto.builder()
                        .answerId(answer.getId())
                        .userId(answer.getUser().getId())
                        .nickname(answer.getUser().getNickname())
                        .content(answer.getContent())
                        .createdAt(answer.getCreatedAt())
                        .build()
                )
                .toList();
    }

    /** 답변 수정 **/
    @Transactional
    public void updateAnswer(Long userId, AnswerUpdateReqDto reqDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        QuestionAnswer answer = questionAnswerRepository
                .findByUserAndQuestion(user, todayQuestion)
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.ANSWER_NOT_FOUND)
                );

        answer.update(
                reqDto.content(),
                reqDto.visibility()
        );
    }

    /** 친구 답변 조회 **/
    @Transactional(readOnly = true)
    public List<FriendAnswerResDto> getFriendsAnswers(Long userId) {

        User me = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        TodayQuestion todayQuestion = todayQuestionRepository
                .findByQuestionDate(LocalDate.now())
                .orElseThrow(() ->
                        new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND)
                );

        // 내 친구 목록 조회
        List<User> friends = friendRepository.findByUser(me)
                .stream()
                .map(Friend::getFriend)
                .toList();

        if (friends.isEmpty()) {
            return List.of();
        }

        // 친구들의 FRIENDS 공개 답변 조회
        return questionAnswerRepository
                .findByQuestionAndVisibilityAndUserIn(
                        todayQuestion,
                        AnswerVisibility.FRIEND,
                        friends
                )
                .stream()
                .map(answer -> FriendAnswerResDto.builder()
                        .answerId(answer.getId())
                        .userId(answer.getUser().getId())
                        .nickname(answer.getUser().getNickname())
                        .content(answer.getContent())
                        .createdAt(answer.getCreatedAt())
                        .build()
                )
                .toList();
    }

    /** 내가 지금까지 작성한 모든 답변 조회 **/
    @Transactional(readOnly = true)
    public List<MyAnswerHistoryResDto> getMyAnswerHistory(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("로그인 사용자 정보가 존재하지 않습니다.")
                );

        return questionAnswerRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(answer -> {
                    TodayQuestion question = answer.getQuestion();

                    return MyAnswerHistoryResDto.builder()
                            .questionId(question.getId())
                            .questionDate(question.getQuestionDate())
                            .questionContent(question.getContent())
                            .answerId(answer.getId())
                            .answerContent(answer.getContent())
                            .visibility(answer.getVisibility())
                            .answeredAt(answer.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
