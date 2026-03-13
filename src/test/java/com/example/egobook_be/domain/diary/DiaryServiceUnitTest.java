package com.example.egobook_be.domain.diary;

import com.example.egobook_be.domain.diary.dto.*;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.ExportFormat;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.diary.service.DiaryExportService;
import com.example.egobook_be.domain.diary.service.DiaryQueryService;
import com.example.egobook_be.domain.diary.service.DiaryService;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.AbilityStat;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.util.InkLogUtil;
import com.example.egobook_be.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryServiceUnitTest {
    @InjectMocks
    private DiaryService diaryService;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private InkLogRepository inkLogRepository;

    @Mock
    private DiaryQueryService diaryQueryService;

    @Mock
    private DiaryExportService diaryExportService;
    @Mock
    private S3Service s3Service;

    @Mock
    private InkLogUtil inkLogUtil;

    @Mock private User user;
    @Mock private Ability ability;
    @Mock private Mission mission;

    @Nested
    @DisplayName("감정 일기 작성 : createdDiary()")
    class createdDiaryTest {

        private final Long USER_ID = 1L;

        @BeforeEach
        void setUp() {
            given(diaryQueryService.getUserById(USER_ID)).willReturn(user);
            given(diaryQueryService.getAbilityByUser(user)).willReturn(ability);
            given(diaryQueryService.getMissionByUser(user)).willReturn(mission);
        }

        @Test
        @DisplayName("고민 일기 작성 시 레벨업이 발생하면 추가 잉크 보상 획득")
        void createDiary_ShouldRewardInk_WhenEmotionRegulationLevelsUp() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION, DiaryType.CONCERN),
                    3,
                    "오늘 감정 조절을 잘했다.",
                    LocalDate.now()
            );

            Diary savedDiary = Diary.builder()
                    .id(1L)
                    .user(user)
                    .date(LocalDate.now())
                    .type(reqDto.type())
                    .content(reqDto.content())
                    .writtenAt(LocalDateTime.now())
                    .build();
            given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);

            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

            // [핵심 해결책]
            // "이미 오늘 쓴 일기가 존재한다(true)"고 설정 -> isFirstDiaryToday = false
            // 이렇게 하면 '첫 일기 보상' 로직을 건너뛰고 '레벨업 보상'만 테스트할 수 있습니다.
            given(diaryRepository.existsByUserAndCreatedAtBetween(
                    any(User.class), any(LocalDateTime.class), any(LocalDateTime.class)
            )).willReturn(true);

            given(diaryRepository.existsByUserAndTypeContainingAndCreatedAtBetween(
                    any(User.class), eq(DiaryType.CONCERN), any(LocalDateTime.class), any(LocalDateTime.class)
            )).willReturn(false);

            // 레벨업 시뮬레이션 (잉크 1 획득)
            given(ability.addEmotionRegulation(1)).willReturn(1);

            AbilityStat level2Stat = AbilityStat.builder().level(2).score(0).build();
            given(ability.getEmotionRegulation()).willReturn(level2Stat);

            // When
            DiaryCreateResDto result = diaryService.createDiary(USER_ID, reqDto);

            // Then
            // 이제 첫 일기 보상이 차단되었으므로, 레벨업 보상 1회만 호출되어야 함
            verify(inkLogUtil).addInkLogToList(any(), eq(user), eq(1), eq(InkLogType.LEVEL_UP));

            List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();
            assertThat(rewards).hasSizeGreaterThanOrEqualTo(2);

            boolean hasLevelUpReward = rewards.stream()
                    .anyMatch(r -> r.message().contains("레벨업")
                            && r.rewardType() == RewardType.INK
                            && r.amount() == 1);
            assertThat(hasLevelUpReward).isTrue();
        }

        @Test
        @DisplayName("고민 일기 작성 시 레벨업하지 않으면 점수 상승 보상 획득")
        void createDiary_ShouldNotRewardExtraInk_WhenEmotionRegulationDoesNotLevelUp() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION, DiaryType.CONCERN),
                    3,
                    "평범한 고민 일기",
                    LocalDate.now()
            );

            Diary savedDiary = Diary.builder()
                    .id(1L)
                    .user(user)
                    .date(LocalDate.now())
                    .type(reqDto.type())
                    .content(reqDto.content())
                    .writtenAt(LocalDateTime.now())
                    .build();
            given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);
            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

            // [핵심 해결책]
            // "이미 오늘 쓴 일기가 존재한다(true)"고 설정 -> isFirstDiaryToday = false
            // 첫 일기 보상 로직 차단
            given(diaryRepository.existsByUserAndCreatedAtBetween(
                    any(User.class), any(LocalDateTime.class), any(LocalDateTime.class)
            )).willReturn(true);

            given(diaryRepository.existsByUserAndTypeContainingAndCreatedAtBetween(
                    any(), eq(DiaryType.CONCERN), any(), any()
            )).willReturn(false);

            // 레벨업 하지 않음 (잉크 0 획득)
            given(ability.addEmotionRegulation(1)).willReturn(0);

            // When
            DiaryCreateResDto result = diaryService.createDiary(USER_ID, reqDto);

            // Then
            // 첫 일기 보상도 없고, 레벨업 보상도 없으므로 호출되지 않아야 함
            verify(user, never()).addInk(1);

            List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();
            boolean hasLevelUpReward = rewards.stream()
                    .anyMatch(r -> r.message().contains("레벨업"));
            assertThat(hasLevelUpReward).isFalse();

            boolean hasScoreReward = rewards.stream()
                    .anyMatch(r -> r.message().contains("스코어가 상승"));
            assertThat(hasScoreReward).isTrue();
        }

        @Test
        @DisplayName("칭찬 일기 작성 시 긍정적 사고 점수가 상승하고 레벨업 시 잉크 획득")
        void createDiary_ShouldRewardInk_WhenPositiveThinkingLevelsUp() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.PRAISE),
                    null,
                    "나를 칭찬해",
                    LocalDate.now()
            );

            given(diaryRepository.save(any(Diary.class))).willReturn(Diary.builder()
                    .id(1L).user(user).date(LocalDate.now()).type(reqDto.type()).content(reqDto.content()).build());
            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

            // 첫 일기 아님
            given(diaryRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(true);

            // 오늘 첫 긍정(칭찬/감사) 일기
            given(diaryRepository.existsByUserAndTypeInAndCreatedAtBetween(
                    any(), any(), any(), any()
            )).willReturn(false);

            // 레벨업 시뮬레이션 (잉크 1 획득)
            given(ability.addPositiveThinking(1)).willReturn(1);
            given(ability.getPositiveThinking()).willReturn(AbilityStat.builder().level(3).build());

            // When
            DiaryCreateResDto result = diaryService.createDiary(USER_ID, reqDto);

            // Then
            List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();

            // 보상 확인
            boolean hasScoreReward = rewards.stream()
                    .anyMatch(r -> r.rewardType() == RewardType.POSITIVE_THINKING && r.amount() == 1);

            boolean hasLevelUpReward = rewards.stream()
                    .anyMatch(r -> r.rewardType() == RewardType.INK && r.message().contains("레벨업"));

            assertThat(hasScoreReward).isTrue();
            assertThat(hasLevelUpReward).isTrue();
        }

        @Test
        @DisplayName("오늘의 첫 일기를 작성하면 기본 잉크 보상 획득")
        void createDiary_ShouldRewardFirstDiaryInk() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    "첫 일기",
                    LocalDate.now()
            );

            given(diaryRepository.save(any(Diary.class))).willReturn(Diary.builder().id(1L).build());
            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);

            // 오늘 작성된 일기가 없음 -> True (isFirstDiaryToday = true)
            given(diaryRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(false);

            // 미션 달성 여부는 False로 설정하여 첫 일기 보상만 확인
            given(mission.updateDailyDiaryMissionStatus(true)).willReturn(false);

            // When
            DiaryCreateResDto result = diaryService.createDiary(USER_ID, reqDto);

            // Then
            verify(inkLogUtil).addInkLogToList(any(), eq(user), eq(1), any());

            boolean hasFirstDiaryReward = result.rewards().stream()
                    .anyMatch(r -> r.message().contains("잉크를 1 획득"));

            assertThat(hasFirstDiaryReward).isTrue();
        }

        @Test
        @DisplayName("일일 미션과 주간 미션을 동시에 달성하면 잉크 대량 획득")
        void createDiary_ShouldRewardHugeInk_WhenWeeklyMissionCompleted() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(Set.of(DiaryType.EMOTION), 3, "미션 달성", LocalDate.now());

            given(diaryRepository.save(any(Diary.class))).willReturn(Diary.builder().id(1L).build());
            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(0);
            given(diaryRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(false); // 오늘 첫 일기

            // 일일 미션 달성 및 주간 미션 달성
            given(mission.updateDailyDiaryMissionStatus(true)).willReturn(true);
            given(mission.isWeeklyMissionCompleted()).willReturn(true);

            // When
            DiaryCreateResDto result = diaryService.createDiary(USER_ID, reqDto);

            // Then
            List<DiaryCreateResDto.RewardResDto> rewards = result.rewards();

            // 총 잉크 보상이 여러 건 발생했는지 확인
            long inkRewardCount = rewards.stream()
                    .filter(r -> r.rewardType() == RewardType.INK)
                    .count();

            assertThat(inkRewardCount).isGreaterThanOrEqualTo(3); // 첫 일기 + 일일 + 주간

            boolean hasWeeklyReward = rewards.stream()
                    .anyMatch(r -> r.message().contains("주간 미션"));
            assertThat(hasWeeklyReward).isTrue();
        }

        @Test
        @DisplayName("하루 일기 작성 제한(48개)을 초과하면 예외 발생")
        void createDiary_ShouldThrowException_WhenLimitExceeded() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    "내용",
                    LocalDate.now()
            );

            // 이미 48개 작성
            given(diaryRepository.countByUserAndDate(any(), any())).willReturn(48);

            // When & Then
            assertThatThrownBy(() -> diaryService.createDiary(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.DIARY_DAILY_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("감정 일기 작성 시 감정 레벨이 없으면 예외 발생")
        void createDiary_ShouldThrowException_WhenEmotionLevelIsNull() {
            // EMOTION 타입인데 level이 null
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    null,
                    "내용",
                    LocalDate.now()
            );

            // When & Then
            assertThatThrownBy(() -> diaryService.createDiary(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.DIARY_EMOTION_LEVEL_REQUIRED);
        }

        @Test
        @DisplayName("작성 가능한 날짜 범위를 벗어나면 예외 발생")
        void createDiary_ShouldThrowException_WhenDateIsFuture() {
            // 미래 날짜
            LocalDate futureDate = LocalDate.now().plusDays(1);

            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    "미래 일기",
                    futureDate
            );

            // When & Then
            assertThatThrownBy(() -> diaryService.createDiary(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.INVALID_DIARY_DATE);
        }

        @Test
        @DisplayName("일기 내용이 400자를 초과하면 예외 발생")
        void createDiary_ShouldThrowException_WhenContentIsTooLong() {
            // 401자 문자열 생성
            String tooLongContent = "가".repeat(401);

            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    tooLongContent,
                    LocalDate.now()
            );

            // When & Then
            assertThatThrownBy(() -> diaryService.createDiary(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.DIARY_TEXT_LIMIT_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("감정 일기 삭제 : deleteDiary()")
    class deleteDiaryTest {

        private final Long USER_ID = 1L;

        @Test
        @DisplayName("감정 일기 삭제 성공")
        void deleteDiary_Success() {
            // Given
            Long diaryId = 100L;
            Diary diary = Diary.builder().id(diaryId).user(user).build();

            given(diaryQueryService.getDiaryWithAuth(USER_ID, diaryId)).willReturn(diary);

            // When
            DiaryDeleteResDto result = diaryService.deleteDiary(USER_ID, diaryId);

            // Then
            verify(diaryRepository).delete(diary);
            assertThat(result.deleted()).isTrue();
        }

        @Test
        @DisplayName("다른 사람의 일기를 삭제하려고 하면 예외 발생")
        void deleteDiary_ShouldThrowException_WhenAccessDenied() {
            // Given
            Long otherUserDiaryId = 999L;

            // QueryService가 권한 없음 예외를 던진다고 가정 (Mocking)
            given(diaryQueryService.getDiaryWithAuth(USER_ID, otherUserDiaryId))
                    .willThrow(new CustomException(DiaryErrorCode.DIARY_ACCESS_DENIED));

            // When & Then
            assertThatThrownBy(() -> diaryService.deleteDiary(USER_ID, otherUserDiaryId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.DIARY_ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 일기를 삭제하려고 하면 예외 발생")
        void deleteDiary_ShouldThrowException_WhenDiaryNotFound() {
            // Given
            Long nonExistentDiaryId = 0L;

            given(diaryQueryService.getDiaryWithAuth(USER_ID, nonExistentDiaryId))
                    .willThrow(new CustomException(DiaryErrorCode.DIARY_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> diaryService.deleteDiary(USER_ID, nonExistentDiaryId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.DIARY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("감정 일기 내보내기 : exportDiaries()")
    class exportDiariesTest {

        private final Long USER_ID = 1L;

        @BeforeEach
        void setUp() {
            given(diaryQueryService.getUserById(USER_ID)).willReturn(user);
        }

        @Test
        @DisplayName("일기 내보내기(PDF) 성공")
        void exportDiaries_Success_PDF() {
            // Given
            DiaryExportReqDto reqDto = new DiaryExportReqDto(
                    ExportFormat.PDF,
                    LocalDate.now().minusDays(5),
                    LocalDate.now()
            );

            List<Diary> diaries = List.of(Diary.builder().id(1L).content("test").build());
            given(diaryQueryService.getDiariesByDate(eq(user), any(), any())).willReturn(diaries);

            byte[] fakePdfBytes = new byte[]{1, 2, 3};
            given(diaryExportService.generatePdf(diaries)).willReturn(fakePdfBytes);

            String fakeUrl = "https://diary/url";
            given(s3Service.uploadTemporaryFile(anyString(), eq(fakePdfBytes), eq("application/pdf")))
                    .willReturn(fakeUrl);

            // When
            DiaryExportResDto result = diaryService.exportDiaries(USER_ID, reqDto);

            // Then
            assertThat(result.fileUrl()).isEqualTo(fakeUrl);
            assertThat(result.format()).isEqualTo(ExportFormat.PDF);
        }

        @Test
        @DisplayName("일기 내보내기(TEXT) 성공")
        void exportDiaries_Success_TEXT() {
            // Given
            DiaryExportReqDto reqDto = new DiaryExportReqDto(
                    ExportFormat.TEXT,
                    LocalDate.now().minusDays(5),
                    LocalDate.now()
            );

            List<Diary> diaries = List.of(Diary.builder().id(1L).content("text content").build());
            given(diaryQueryService.getDiariesByDate(eq(user), any(), any())).willReturn(diaries);

            // 텍스트 변환 Mocking
            byte[] fakeTextBytes = "일기 내용...".getBytes();
            given(diaryExportService.generateText(diaries)).willReturn(fakeTextBytes);

            String fakeUrl = "https://diary/text-url";
            // contentType이 "text/plain"으로 넘어가는지 검증
            given(s3Service.uploadTemporaryFile(anyString(), eq(fakeTextBytes), eq("text/plain")))
                    .willReturn(fakeUrl);

            // When
            DiaryExportResDto result = diaryService.exportDiaries(USER_ID, reqDto);

            // Then
            assertThat(result.fileUrl()).isEqualTo(fakeUrl);
            assertThat(result.format()).isEqualTo(ExportFormat.TEXT);
        }

        @Test
        @DisplayName("내보내기 날짜 범위가 잘못되면(시작일이 종료일보다 늦으면) 예외 발생")
        void exportDiaries_ShouldThrowException_WhenDateRangeIsInvalid() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().minusDays(1); // 종료일이 더 과거

            DiaryExportReqDto reqDto = new DiaryExportReqDto(
                    ExportFormat.TEXT,
                    startDate,
                    endDate
            );

            // QueryService가 날짜 검증 로직에서 예외를 던진다고 가정 (Mocking)
            given(diaryQueryService.getDiariesByDate(eq(user), eq(startDate), eq(endDate)))
                    .willThrow(new CustomException(DiaryErrorCode.EXPORT_INVALID_DATE_RANGE));

            // When & Then
            assertThatThrownBy(() -> diaryService.exportDiaries(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.EXPORT_INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("내보낼 일기가 없으면 예외 발생")
        void exportDiaries_ShouldThrowException_WhenNoDiaries() {
            // Given
            DiaryExportReqDto reqDto = new DiaryExportReqDto(
                    ExportFormat.TEXT,
                    LocalDate.now().minusDays(5),
                    LocalDate.now()
            );

            // 빈 리스트 반환
            given(diaryQueryService.getDiariesByDate(eq(user), any(), any())).willReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> diaryService.exportDiaries(USER_ID, reqDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DiaryErrorCode.NO_DIARY_TO_EXPORT);
        }

        @Test
        @DisplayName("S3 파일 업로드가 실패하면 예외 발생")
        void exportDiaries_ShouldThrowException_WhenS3UploadFails() {
            // Given
            DiaryExportReqDto reqDto = new DiaryExportReqDto(
                    ExportFormat.PDF,
                    LocalDate.now().minusDays(1),
                    LocalDate.now()
            );

            List<Diary> diaries = List.of(Diary.builder().id(1L).content("test").build());
            given(diaryQueryService.getDiariesByDate(eq(user), any(), any())).willReturn(diaries);

            // PDF 생성까진 성공
            given(diaryExportService.generatePdf(diaries)).willReturn(new byte[10]);

            // S3 업로드 시 런타임 예외 발생 가정 (Mocking)
            given(s3Service.uploadTemporaryFile(anyString(), any(), anyString()))
                    .willThrow(new RuntimeException("S3 Connection Error"));

            // When & Then
            // S3 에러가 발생했을 때 Service가 멈추지 않고 예외를 뱉는지 확인
            assertThatThrownBy(() -> diaryService.exportDiaries(USER_ID, reqDto))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}