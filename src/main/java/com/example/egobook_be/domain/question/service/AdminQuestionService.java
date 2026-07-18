package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AdminQuestionReqDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionResDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionListResDto;
import com.example.egobook_be.domain.question.dto.AdminTodayAnswerGroupResDto;
import com.example.egobook_be.domain.question.dto.AdminTodayAnswerItemResDto;
import com.example.egobook_be.domain.question.dto.AdminTodayAnswerListResDto;
import com.example.egobook_be.domain.question.entity.QuestionAnswer;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.QuestionAnswerRepository;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.egobook_be.global.exception.CustomException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private final TodayQuestionRepository todayQuestionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;

    @Transactional
    public AdminQuestionResDto createQuestion(AdminQuestionReqDto reqDto) {

        if (todayQuestionRepository.existsByQuestionDateAndDeletedAtIsNull(reqDto.questionDate())) {
            throw new CustomException(QuestionErrorCode.DUPLICATE_QUESTION_DATE);
        }

        TodayQuestion question = TodayQuestion.builder()
                .content(reqDto.content())
                .questionDate(reqDto.questionDate())
                .build();
        TodayQuestion saved = todayQuestionRepository.save(question);
        return new AdminQuestionResDto(saved.getId(), saved.getContent(), saved.getQuestionDate(), saved.getCreatedAt(), question.getDeletedAt());
    }

    @Transactional(readOnly = true)
    public AdminQuestionListResDto getQuestionList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("questionDate").descending());
        Slice<TodayQuestion> slice = todayQuestionRepository.findAllByDeletedAtIsNull(pageable);

        List<AdminQuestionListResDto.AdminQuestionItemResDto> items = slice.getContent().stream()
                .map(q -> new AdminQuestionListResDto.AdminQuestionItemResDto(q.getId(), q.getContent(), q.getQuestionDate(), q.getCreatedAt()))
                .toList();

        return new AdminQuestionListResDto(items, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public AdminQuestionResDto getQuestionDetail(Long id) {
        TodayQuestion question = todayQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.INVALID_QUESTION_ID));
        return new AdminQuestionResDto(question.getId(), question.getContent(), question.getQuestionDate(), question.getCreatedAt(), question.getDeletedAt());
    }

    @Transactional
    public AdminQuestionResDto updateQuestion(Long id, AdminQuestionReqDto reqDto) {
        TodayQuestion oldQuestion = todayQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.TODAY_QUESTION_NOT_FOUND));

        // 기존 질문을 소프트 딜리트 처리
        oldQuestion.delete();

        if (todayQuestionRepository.existsByQuestionDateAndDeletedAtIsNull(reqDto.questionDate())) {
            throw new CustomException(QuestionErrorCode.DUPLICATE_QUESTION_DATE);
        }

        // 새로운 질문 엔티티 생성 및 저장
        TodayQuestion newQuestion = TodayQuestion.builder()
                .content(reqDto.content())
                .questionDate(reqDto.questionDate())
                .build();

        TodayQuestion saved = todayQuestionRepository.save(newQuestion);

        return new AdminQuestionResDto(
                saved.getId(),
                saved.getContent(),
                saved.getQuestionDate(),
                saved.getCreatedAt(),
                saved.getDeletedAt()
        );
    }

    @Transactional
    public void deleteQuestion(Long id) {
        TodayQuestion question = todayQuestionRepository.findById(id)
                .orElseThrow(() -> new CustomException(QuestionErrorCode.INVALID_QUESTION_ID));
        question.delete();
    }
    
    /**
     * 기간 내 오늘의 질문과, 그 질문에 공개 동의(PUBLIC)한 유저의 답변을 함께 조회한다.
     * - startDate/endDate 미입력 시 오늘이 속한 달(1일 ~ 말일)을 기본값으로 사용
     * - 질문은 날짜 내림차순(최신이 상단), 답변은 질문 내에서 작성일 내림차순으로 정렬
     * @param startDate : 조회 시작일 (nullable)
     * @param endDate : 조회 종료일 (nullable)
     * @return : 질문별로 그룹핑된 공개 답변 목록
     */
    @Transactional(readOnly = true)
    public AdminTodayAnswerListResDto getTodayAnswers(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate resolvedEnd = endDate != null ? endDate : today.with(TemporalAdjusters.lastDayOfMonth());

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new CustomException(QuestionErrorCode.INVALID_DATE_RANGE);
        }

        List<TodayQuestion> questions = todayQuestionRepository
                .findAllByQuestionDateBetweenAndDeletedAtIsNullOrderByQuestionDateDesc(resolvedStart, resolvedEnd);

        List<QuestionAnswer> publicAnswers = questionAnswerRepository
                .findAllByQuestionDateBetweenAndVisibility(resolvedStart, resolvedEnd, AnswerVisibility.PUBLIC);

        Map<Long, List<AdminTodayAnswerItemResDto>> answersByQuestionId = publicAnswers.stream()
                .collect(Collectors.groupingBy(
                        qa -> qa.getQuestion().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(qa -> new AdminTodayAnswerItemResDto(
                                qa.getId(),
                                qa.getContent(),
                                qa.getCreatedAt(),
                                qa.getUser().getAccountCode()
                        ), Collectors.toList())
                ));

        List<AdminTodayAnswerGroupResDto> groups = questions.stream()
                .map(q -> new AdminTodayAnswerGroupResDto(
                        q.getId(),
                        q.getQuestionDate(),
                        q.getContent(),
                        answersByQuestionId.getOrDefault(q.getId(), List.of())
                ))
                .toList();

        return new AdminTodayAnswerListResDto(resolvedStart, resolvedEnd, groups);
    }
}