package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.dto.WordCloudDto;
import com.example.egobook_be.domain.ego_room.entity.UserStats;
import com.example.egobook_be.domain.ego_room.repository.UserStatsRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EgoStatsServiceTest {

    @Mock
    private UserStatsRepository userStatsRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private WordCloudService wordCloudAiService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private EgoStatsService egoStatsService;



    @Test
    @DisplayName("월통계를 계산하고 JSON 형식으로 저장")
    void calculateAndSaveStats_validPeriod_success() throws Exception {
        // given
        Long userId = 1L;
        int year = 2026;
        int month = 3;
        User user = User.builder().id(userId).build();
        Diary diary = Diary.builder().writtenAt(LocalDateTime.now()).emotionLevel(3).build();

        when(diaryRepository.findAllByUserIdAndWrittenAtAfter(any(), any())).thenReturn(List.of(diary));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(wordCloudAiService.calculateWordCloud(any())).thenReturn(List.of(new WordCloudDto("행복", 10)));
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when
        egoStatsService.calculateAndSaveStats(userId, year, month);

        // then
        verify(userStatsRepository, times(1)).save(any(UserStats.class));
    }

    @Test
    @DisplayName("calculateAndSaveStats_유저없음_실패")
    void calculateAndSaveStats_userNotFound_fail() {
        // given
        Long userId = 999L; // 없는 유저 ID
        int year = 2026;
        int month = 3;


        when(diaryRepository.findAllByUserIdAndWrittenAtAfter(any(), any())).thenReturn(Collections.emptyList());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(RuntimeException.class, () -> {
            egoStatsService.calculateAndSaveStats(userId, year, month);
        });
    }

}