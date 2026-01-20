package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.ExportFormat;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.mapper.DiaryMapper;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import com.example.egobook_be.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryExportService diaryExportService;
    private final DiaryQueryService diaryQueryService;
    private final S3Service s3Service;

    /** 감정 일기 생성 */
    @Transactional
    public DiaryCreateResDto createDiary(Long userId, DiaryCreateReqDto dto) {

        User user = diaryQueryService.getUserById(userId);

        // 일기 타입 선택 검증
        if (dto.type() == null || dto.type().isEmpty()) {
            throw new CustomException(DiaryErrorCode.DIARY_TYPE_REQUIRED);
        }

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

        Ability ability = user.getAbility();

        if (isFirstConcernToday) {
            ability.addEmotionRegulation(1);
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.EMOTION_REGULATION, 1, "고민 일기를 작성하여 감정조절이 상승했어요"
            ));
        }

        if (isFirstPositiveToday) {
            int amount = 1;
            ability.addPositiveThinking(1);
            if (dto.type().contains(DiaryType.PRAISE)) {
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.POSITIVE_THINKING, amount, "오늘 첫 칭찬 일기를 작성하여 긍정적 사고가 상승했어요"
                ));
                amount = 0;
            }
            if (dto.type().contains(DiaryType.GRATITUDE)) {
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.POSITIVE_THINKING, amount, "오늘 첫 감사 일기를 작성하여 긍정적 사고가 상승했어요"
                ));
            }
        }

        return DiaryMapper.toDiaryCreateDto(diary, rewards);
    }

    /** 감정 일기 상세 조회 */
    public DiaryResDto getDiary(Long userId, Long diaryId) {

        Diary diary = diaryQueryService.getDiaryWithAuth(userId, diaryId);

        return DiaryMapper.toDiaryDto(diary);
    }

    /** 감정 일기 수정 */
    @Transactional
    public DiaryResDto updateDiary(Long userId, Long diaryId, DiaryUpdateReqDto dto) {

        Diary diary = diaryQueryService.getDiaryWithAuth(userId, diaryId);

        // 일기 타입 선택 검증
        if (dto.type() == null || dto.type().isEmpty()) {
            throw new CustomException(DiaryErrorCode.DIARY_TYPE_REQUIRED);
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

        return DiaryMapper.toDiaryDto(diary);
    }

    /** 감정 일기 삭제 */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {

        Diary diary = diaryQueryService.getDiaryWithAuth(userId, diaryId);

        diaryRepository.delete(diary);
    }

    /** 날짜별 감정 일기 목록 조회 (type 필터링) */
    @Transactional(readOnly = true)
    public DiaryListResDto getDiaries(Long userId, LocalDate date, DiaryType type, int page, int size) {

        User user = diaryQueryService.getUserById(userId);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "writtenAt")
        );

        Slice<Diary> slice = diaryRepository.findAllByUserAndTypeAndWrittenAtBetween(
                user,
                type,
                startOfDay,
                endOfDay,
                pageable
        );

        SliceResponse<DiaryResDto> diaries = SliceResponse.of(slice, DiaryMapper::toDiaryDto);

        // 오늘 작성한 일기 개수 계산
        int dailyCount = diaryRepository.countByUserAndWrittenAtBetween(user, startOfDay, endOfDay);

        return DiaryMapper.toDiaryListDto(diaries, dailyCount);
    }

    /** 감정 일기 달력 */
    @Transactional(readOnly = true)
    public DiaryCalendarResDto getDiaryCalendar(Long userId, YearMonth month) {

        User user = diaryQueryService.getUserById(userId);

        LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(LocalTime.MAX);

        List<DiaryRepository.DailyEmotionCount> dailyEmotions =
                diaryRepository.findDailyEmotions(user, startOfMonth, endOfMonth);

        return DiaryMapper.toDiaryCalendarDto(month, dailyEmotions);
    }

    /** 감정 일기 내보네기 */
    public DiaryExportResDto exportDiaries(Long userId, DiaryExportReqDto dto) {

        User user = diaryQueryService.getUserById(userId);
        List<Diary> diaries = diaryQueryService.getDiariesByDate(user, dto.startDate(), dto.endDate());

        if (diaries.isEmpty()) {
            throw new CustomException(DiaryErrorCode.NO_DIARY_TO_EXPORT);
        }

        // 파일 생성
        byte[] fileContent;
        String fileName;
        String contentType;

        if (dto.format() == ExportFormat.PDF) {
            fileContent = diaryExportService.generatePdf(diaries);
            fileName = String.format("에고북_%s.pdf", LocalDate.now());
            contentType = "application/pdf";
        } else {
            fileContent = diaryExportService.generateText(diaries);
            fileName = String.format("에고북_%s.txt", LocalDate.now());
            contentType = "text/plain";
        }

        // S3 업로드 (임시 파일, 24시간 후 만료)
        String s3Key = String.format("diary-exports/%d/%s", userId, fileName);
        String fileUrl = s3Service.uploadTemporaryFile(s3Key, fileContent, contentType);

        // 만료 시간 설정 (24시간)
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        return DiaryMapper.toDiaryExportDto(
                fileUrl, expiresAt, dto.format(), dto.startDate(), dto.endDate()
        );
    }
}
