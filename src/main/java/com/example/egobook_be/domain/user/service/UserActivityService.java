package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.user.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityLogRepository userActivityLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDailyActivity(Long userId) {
        try {
            userActivityLogRepository.insertIgnore(userId, LocalDate.now());
        } catch (Exception e) {
            log.warn("활동 로그 저장 실패 userId={}", userId, e);
        }
    }
}
