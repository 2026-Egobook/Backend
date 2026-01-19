package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;

    /** 감정 일기 생성 */
    @Transactional
    public DiaryCreateResDto createDiary(Long userId, DiaryCreateReqDto dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.USER_NOT_FOUND));

        // '감정(EMOTION)' 일기 감정 레벨 검증
        if (dto.type().contains(DiaryType.EMOTION)) {
            if (dto.emotionLevel() == null) {
                throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_REQUIRED);
            }

            if (dto.emotionLevel() < 1 || dto.emotionLevel() > 5) {
                throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_INVALID);
            }
        } else if (dto.emotionLevel() != null) {
            throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_NOT_ALLOWED);
        }

        // 일기 400자 이하 검증
        if (dto.content() != null && dto.content().length() > 400) {
            throw new CustomException(DiaryErrorCode.DIARY_TEXT_LIMIT_EXCEEDED);
        }

        // 일기 저장 날짜 설정
        LocalDateTime writtenAt = (dto.date() != null) ?
                LocalDateTime.of(dto.date(), LocalTime.now()) : LocalDateTime.now();

        LocalDateTime startOfDay = writtenAt.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = writtenAt.toLocalDate().atTime(LocalTime.MAX);

        // 일기 작성 가능 여부
        if (diaryRepository.countByUserAndWrittenAtBetween(user, startOfDay, endOfDay) >= 48) {
            throw new CustomException(DiaryErrorCode.DIARY_DAILY_LIMIT_EXCEEDED);
        }

        List<DiaryCreateResDto.RewardResDto> rewards = new ArrayList<>();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        // 오늘 첫 일기 작성 여부
        boolean isFirstToday = !diaryRepository.existsByUserAndCreatedAtBetween(user, startOfToday, endOfToday);

        // 오늘 첫 고민(CONCERN) 일기 작성 여부
        boolean isFirstConcernToday =
                dto.type().contains(DiaryType.CONCERN) &&
                        !diaryRepository.existsByUserAndTypeContainingAndCreatedAtBetween(
                                user, DiaryType.CONCERN, startOfToday, endOfToday
                        );

        // 오늘 첫 칭찬(PRAISE) 또는 감사(GRATITUDE) 작성 여부
        boolean isFirstPositiveToday =
                (dto.type().contains(DiaryType.PRAISE) || dto.type().contains(DiaryType.GRATITUDE)) &&
                        !diaryRepository.existsByUserAndTypeInAndCreatedAtBetween(
                                user, Set.of(DiaryType.PRAISE, DiaryType.GRATITUDE),
                                startOfToday, endOfToday
                        );

        Diary diary = diaryRepository.save(Diary.builder()
                .user(user)
                .type(dto.type())
                .content(dto.content())
                .emotionLevel(dto.emotionLevel())
                .writtenAt(writtenAt)
                .build());

        // 리워드 지급
        if (isFirstToday) {
            user.addInk(1);
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.INK, 1, "잉크를 1 획득했어요"
            ));
        }

        if (isFirstConcernToday) {
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.EMOTION_REGULATION, 1, "고민 일기를 작성하여 감정조절이 상승했어요"
            ));
        }

        if (isFirstPositiveToday) {
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.POSITIVE_THINKING, 1, "감사 혹은 칭찬 일기를 작성하여 긍정적 사고가 상승했어요"
            ));
        }

        return DiaryCreateResDto.builder()
                .entry(DiaryCreateResDto.DiaryEntryResDto.builder()
                        .diaryId(diary.getId())
                        .date(diary.getWrittenAt().toLocalDate())
                        .writtenAt(diary.getWrittenAt())
                        .type(diary.getType())
                        .emotionLevel(diary.getEmotionLevel())
                        .content(diary.getContent())
                        .createdAt(diary.getCreatedAt())
                        .updatedAt(diary.getUpdatedAt())
                        .build())
                .rewards(rewards)
                .build();
    }

    /** 감정 일기 상세 조회 */
    @Transactional(readOnly = true)
    public DiaryResDto getDiary(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new CustomException(DiaryErrorCode.DIARY_ACCESS_DENIED);
        }

        return DiaryResDto.builder()
                .diaryId(diary.getId())
                .date(diary.getWrittenAt().toLocalDate())
                .writtenAt(diary.getWrittenAt())
                .type(diary.getType())
                .emotionLevel(diary.getEmotionLevel())
                .content(diary.getContent())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    /** 감정 일기 수정 */
    @Transactional
    public DiaryResDto updateDiary(Long userId, Long diaryId, DiaryUpdateReqDto dto) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new CustomException(DiaryErrorCode.DIARY_ACCESS_DENIED);
        }

        // '감정(EMOTION)' 일기 감정 레벨 검증
        if (diary.getType().contains(DiaryType.EMOTION)) {
            if (dto.emotionLevel() == null) {
                throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_REQUIRED);
            }
            if (dto.emotionLevel() < 1 || dto.emotionLevel() > 5) {
                throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_INVALID);
            }
        } else if (dto.emotionLevel() != null) {
            throw new CustomException(DiaryErrorCode.DIARY_EMOTION_LEVEL_NOT_ALLOWED);
        }

        diary.update(dto.content(), dto.type(), dto.emotionLevel());
        diaryRepository.save(diary);

        return DiaryResDto.builder()
                .diaryId(diary.getId())
                .date(diary.getWrittenAt().toLocalDate())
                .writtenAt(diary.getWrittenAt())
                .type(diary.getType())
                .emotionLevel(diary.getEmotionLevel())
                .content(diary.getContent())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    /** 감정 일기 삭제 */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new CustomException(DiaryErrorCode.DIARY_ACCESS_DENIED);
        }

        diaryRepository.delete(diary);
    }

    /** 날짜별 감정 일기 목록 조회 (type 필터링) */
    @Transactional(readOnly = true)
    public DiaryListResDto getDiaries(Long userId, LocalDate date, DiaryType type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.USER_NOT_FOUND));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Diary> diaries = diaryRepository.findAllByUserAndTypeAndWrittenAtBetween(
                user, type, startOfDay, endOfDay
        );

        int dailyCount = diaryRepository.countByUserAndWrittenAtBetween(user, startOfDay, endOfDay);

        return DiaryListResDto.builder()
                .diaries(diaries.stream()
                        .map(diary -> DiaryResDto.builder()
                                .diaryId(diary.getId())
                                .date(diary.getWrittenAt().toLocalDate())
                                .writtenAt(diary.getWrittenAt())
                                .type(diary.getType())
                                .emotionLevel(diary.getEmotionLevel())
                                .content(diary.getContent())
                                .createdAt(diary.getCreatedAt())
                                .updatedAt(diary.getUpdatedAt())
                                .build()
                        ).toList()
                )
                .dailyCount(dailyCount)
                .build();
    }
}
