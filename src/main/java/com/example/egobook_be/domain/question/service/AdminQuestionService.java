package com.example.egobook_be.domain.question.service;

import com.example.egobook_be.domain.question.dto.AdminQuestionReqDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionResDto;
import com.example.egobook_be.domain.question.dto.AdminQuestionListResDto;
import com.example.egobook_be.domain.question.entity.TodayQuestion;
import com.example.egobook_be.domain.question.exception.QuestionErrorCode;
import com.example.egobook_be.domain.question.repository.TodayQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.egobook_be.global.exception.CustomException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private final TodayQuestionRepository todayQuestionRepository;

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
}