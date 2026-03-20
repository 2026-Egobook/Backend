package com.example.egobook_be.domain.diary;

import com.example.egobook_be.domain.diary.dto.DiaryCreateReqDto;
import com.example.egobook_be.domain.diary.dto.DiaryCreateResDto;
import com.example.egobook_be.domain.diary.dto.DiaryDeleteResDto;
import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.diary.service.DiaryService;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DiaryServiceIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AbilityRepository abilityRepository;

    @Autowired
    private MissionRepository missionRepository;

    @MockitoBean
    private S3Service s3Service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .accountCode("test-account")
                .nickname("에고북777")
                .ink(0)
                .build());

        abilityRepository.save(Ability.builder()
                .user(testUser)
                .build());

        missionRepository.save(Mission.builder()
                .user(testUser)
                .build());
    }

    @Nested
    @DisplayName("감정 일기 작성 : createDiary()")
    class CreateDiaryIntegrationTest {

        @Test
        @DisplayName("일기 작성 시 DB 저장")
        void createDiary_ShouldSaveToDatabase() {
            // Given
            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    "일기 작성 테스트",
                    LocalDate.now()
            );

            // When
            diaryService.createDiary(testUser.getId(), reqDto);

            // Then
            List<Diary> diaries = diaryRepository.findAll();
            assertThat(diaries).hasSize(1);
            assertThat(diaries.getFirst().getContent()).isEqualTo("일기 작성 테스트");
            assertThat(diaries.getFirst().getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("오늘 첫 일기 작성 시 잉크 증가")
        void createDiary_ShouldIncreaseInk_WhenFirstDiaryToday() {
            // Given
            int ink = testUser.getInk();

            DiaryCreateReqDto reqDto = new DiaryCreateReqDto(
                    Set.of(DiaryType.EMOTION),
                    3,
                    "첫 일기",
                    LocalDate.now()
            );

            // When
            DiaryCreateResDto result = diaryService.createDiary(testUser.getId(), reqDto);

            // Then
            // 잉크 보상이 응답에 포함되었는지 확인
            assertThat(result.rewards()).isNotEmpty();

            // DB 유저 잉크 증가 확인
            User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(updatedUser.getInk()).isGreaterThan(ink);
        }
    }

    @Nested
    @DisplayName("감정 일기 삭제 : deleteDiary()")
    class DeleteDiaryIntegrationTest {

        @Test
        @DisplayName("일기 삭제 시 DB에서 제거")
        void deleteDiary_ShouldRemoveFromDatabase() {
            // Given
            Diary diary = diaryRepository.save(Diary.builder()
                    .user(testUser)
                    .content("삭제 예정 일기")
                    .date(LocalDate.now())
                    .type(Set.of(DiaryType.EMOTION))
                    .emotionLevel(3)
                    .build());

            Long diaryId = diary.getId();

            // When
            DiaryDeleteResDto result = diaryService.deleteDiary(testUser.getId(), diaryId);

            // Then
            assertThat(result.deleted()).isTrue();

            // DB 삭제 확인
            Optional<Diary> deletedDiary = diaryRepository.findById(diaryId);
            assertThat(deletedDiary).isEmpty();
        }

        @Test
        @DisplayName("일기 삭제 후 해당 유저의 일기 목록에서 제거")
        void deleteDiary_ShouldNotAppearInUserDiaryList() {
            // Given
            Diary diary1 = diaryRepository.save(Diary.builder()
                    .user(testUser)
                    .content("일기 1")
                    .date(LocalDate.now())
                    .type(Set.of(DiaryType.EMOTION))
                    .emotionLevel(3)
                    .build());

            diaryRepository.save(Diary.builder()
                    .user(testUser)
                    .content("일기 2")
                    .date(LocalDate.now())
                    .type(Set.of(DiaryType.EMOTION))
                    .emotionLevel(3)
                    .build());

            // When - 일기 1 삭제
            diaryService.deleteDiary(testUser.getId(), diary1.getId());

            // Then - 일기 2 있는지 확인
            List<Diary> remainingDiaries = diaryRepository.findAll();
            assertThat(remainingDiaries).hasSize(1);
            assertThat(remainingDiaries.getFirst().getContent()).isEqualTo("일기 2");
        }
    }
}