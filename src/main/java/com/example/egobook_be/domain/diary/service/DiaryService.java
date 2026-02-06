package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.ExportFormat;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.mapper.DiaryMapper;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import com.example.egobook_be.global.util.InkLogUtil;
import com.example.egobook_be.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final InkLogRepository inkLogRepository;
    private final DiaryExportService diaryExportService;
    private final DiaryQueryService diaryQueryService;
    private final S3Service s3Service;
    private final InkLogUtil inkLogUtil;

    /** 감정 일기 생성 */
    @Transactional
    public DiaryCreateResDto createDiary(Long userId, DiaryCreateReqDto dto) {
        User user = diaryQueryService.getUserById(userId);
        Ability ability = diaryQueryService.getAbilityByUser(user);
        Mission userMission = diaryQueryService.getMissionByUser(user);

        // DiaryCreateReqDto 검증
        verifyDiaryCreateReqDto(dto);

        // 일기 저장 날짜 설정
        LocalDateTime writtenAt = (dto.dateTime() != null) ?
                dto.dateTime() : LocalDateTime.now();

        LocalDate diaryDate = writtenAt.toLocalDate();

        // 일기 작성 가능 여부
        if (diaryRepository.countByUserAndDate(user, diaryDate) >= 48) {
            throw new CustomException(DiaryErrorCode.DIARY_DAILY_LIMIT_EXCEEDED);
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        // 오늘 첫 일기 작성 여부
        boolean isFirstDiaryToday = !diaryRepository.existsByUserAndCreatedAtBetween(user, startOfToday, endOfToday);

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
                .date(diaryDate)
                .type(dto.type())
                .content(dto.content())
                .emotionLevel(dto.emotionLevel())
                .writtenAt(writtenAt)
                .build());

       
        // Reward 객체들 생성 후 DiaryCreateResDto 반환
        return DiaryMapper.toDiaryCreateDto(diary, getRewards(user, ability, userMission, isFirstDiaryToday, isFirstConcernToday, isFirstPositiveToday, dto.type()));
    }

    /** 조건에 따라 잉크, 능력치 부여 & 미션 갱신을 수행한 뒤 Reward 객체들을 생성하는 함수 */
    private List<DiaryCreateResDto.RewardResDto> getRewards(User user, Ability ability, Mission userMission, boolean isFirstDiaryToday, boolean isFirstConcernToday, boolean isFirstPositiveToday, Set<DiaryType> diaryTypes) {
        List<DiaryCreateResDto.RewardResDto> rewards = new ArrayList<>();
        List<InkLog> inkLogs = new ArrayList<>();
        /*
         * 1. 오늘 처음 감정 일기를 작성한 경우
         * - 잉크 +1
         * - 잉크 로그 추가
         * - 일일 미션 상태 업데이트
         */
        if (isFirstDiaryToday) {
            inkLogUtil.addInkLog(inkLogs, user, 1, InkLogType.FIRST_EMOTION_DIARY); // 사용자에게 잉크 추가 & 잉크 로그 추가해주는 함수
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.INK, 1, "잉크를 1 획득했어요"
            ));
            /*
             * 1-1. 만약 이번이 처음 일일 미션을 수행한 경우일 때
             * - 잉크 +1
             * - 잉크 로그 추가
             * - reward 객체 추가
             */
            if(userMission.updateDailyDiaryMissionStatus(true)){
                inkLogUtil.addInkLog(inkLogs, user, 1, InkLogType.DAILY_MISSION_REWARD);
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.INK, 1, "일일 미션 성공으로 잉크를 1 획득했어요"
                ));
                /*
                 * 1-2. 만약 오늘이 일일 미션을 7일째 완료한 날인 경우
                 * - 잉크 +2
                 * - 잉크 로그 추가
                 * - reward 객체 추가
                 */
                if(userMission.isWeeklyMissionCompleted()){
                    inkLogUtil.addInkLog(inkLogs, user, 2, InkLogType.WEEKLY_MISSION_REWARD);
                    rewards.add(new DiaryCreateResDto.RewardResDto(
                            RewardType.INK, 2, "주간 미션 성공으로 잉크를 추가로 2 획득했어요"
                    ));
                }
            }
        }

        /*
         * 2. 오늘 처음 고민 일기를 작성한 경우
         * - 감정조절 +1 Score
         * - 감정조절 레벨이 올랐으면 잉크 +1
         */
        if (isFirstConcernToday) {
            int earnedInk = ability.addEmotionRegulation(1); // 감정 조절 Score를 증가시켰을 때, 사용자가 레벨업한 경우 earnedInk의 값은 1이다
            rewards.add(new DiaryCreateResDto.RewardResDto(
                    RewardType.EMOTION_REGULATION, 1, "고민 일기를 작성하여 감정조절 스코어가 상승했어요"
            ));
            // 2-1. 감정 조절 레벨이 올랐는지 확인
            if(earnedInk == 1){
                inkLogUtil.addInkLog(inkLogs, user, earnedInk, InkLogType.LEVEL_UP);
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.INK, earnedInk, "[감정 조절 레벨업] 잉크를 추가로 1 획득했어요"+"(현재 감정 조절 레벨: " + ability.getEmotionRegulation() + ")"
                ));
            }
        }

        /*
         * 3. 오늘 처음 감사/칭찬 일기를 작성한 경우
         * - 긍정사고 +1칸
         * - 긍정 사고의 레벨이 올랐으면 잉크 +1
         */
        if (isFirstPositiveToday) {
            int amount = 1;
            int earnedInk = ability.addPositiveThinking(amount);
            if (diaryTypes.contains(DiaryType.PRAISE)) {
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.POSITIVE_THINKING, amount, "오늘 첫 칭찬 일기를 작성하여 긍정적 사고 스코어가 상승했어요"
                ));
                amount = 0;
            }
            if (diaryTypes.contains(DiaryType.GRATITUDE)) {
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.POSITIVE_THINKING, amount, "오늘 첫 감사 일기를 작성하여 긍정적 사고 스코어가 상승했어요"
                ));
            }
            // 3-1. 긍정 사고의 레벨이 올랐는지 확인
            if(earnedInk == 1){
                inkLogUtil.addInkLog(inkLogs, user, earnedInk, InkLogType.LEVEL_UP);
                rewards.add(new DiaryCreateResDto.RewardResDto(
                        RewardType.INK, earnedInk, "[긍정 사고 레벨업] 잉크를 추가로 1 획득했어요"+"(현재 긍정 사고 레벨: " + ability.getPositiveThinking() + ")"
                ));
            }
        }

        // 지금까지의 과정에서 생성된 InkLog들 일괄 저장
        inkLogRepository.saveAll(inkLogs);
        return rewards;
    }

    private void verifyDiaryCreateReqDto(DiaryCreateReqDto dto){
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
    public DiaryDeleteResDto deleteDiary(Long userId, Long diaryId) {

        Diary diary = diaryQueryService.getDiaryWithAuth(userId, diaryId);

        diaryRepository.delete(diary);

        return DiaryMapper.toDiaryDeleteDto(true);
    }

    /** 날짜별 감정 일기 목록 조회 (type 필터링) */
    @Transactional(readOnly = true)
    public DiaryListResDto getDiaries(Long userId, LocalDate date, DiaryType type, int page, int size) {

        User user = diaryQueryService.getUserById(userId);

        if (page < 1) {
            throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        }

        if (size < 1 || size > 100) {
            throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "writtenAt")
        );

        Slice<Diary> slice = diaryRepository.findAllByUserAndTypeAndDate(
                user,
                type,
                date,
                pageable
        );

        SliceResponse<DiaryResDto> diaries = SliceResponse.of(slice, DiaryMapper::toDiaryDto);

        // 선택 날짜 일기 개수
        int dailyCount = diaryRepository.countByUserAndDate(user, date);

        return DiaryMapper.toDiaryListDto(diaries, dailyCount);
    }

    /** 감정 일기 달력 */
    @Transactional(readOnly = true)
    public DiaryCalendarResDto getDiaryCalendar(Long userId, YearMonth month) {

        User user = diaryQueryService.getUserById(userId);

        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        List<DiaryRepository.DailyEmotionCount> dailyEmotions =
                diaryRepository.findDailyEmotions(user, startOfMonth, endOfMonth);

        return DiaryMapper.toDiaryCalendarDto(month, dailyEmotions);
    }

    /** 감정 일기 내보내기 */
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
