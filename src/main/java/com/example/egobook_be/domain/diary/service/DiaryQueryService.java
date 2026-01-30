package com.example.egobook_be.domain.diary.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryQueryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final AbilityRepository abilityRepository;

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Ability getAbilityByUser(User user) {
        return abilityRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(UserErrorCode.ABILITY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Mission getMissionByUser(User user) {
        return missionRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.MISSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Diary getDiaryWithAuth(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new CustomException(DiaryErrorCode.DIARY_ACCESS_DENIED);
        }

        return diary;
    }

    @Transactional(readOnly = true)
    public List<Diary> getDiariesByDate(User user, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // 미래 날짜 검증
        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            throw new CustomException(DiaryErrorCode.EXPORT_FUTURE_DATE_NOT_ALLOWED);
        }

        // 날짜 순서 검증
        if (startDate.isAfter(endDate)) {
            throw new CustomException(DiaryErrorCode.EXPORT_INVALID_DATE_RANGE);
        }

        // 최대 1년 범위 검증
        if (startDate.plusYears(1).isBefore(endDate)) {
            throw new CustomException(DiaryErrorCode.EXPORT_RANGE_EXCEEDS_ONE_YEAR);
        }

        LocalDateTime startOfDate = startDate.atStartOfDay();
        LocalDateTime endOfDate = endDate.atTime(LocalTime.MAX);

        return diaryRepository.findAllByUserAndWrittenAtBetween(user, startOfDate, endOfDate);
    }
}
