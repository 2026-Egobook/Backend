package com.example.egobook_be.domain.notice.scheduler;

import com.example.egobook_be.domain.notice.entity.Notice;
import com.example.egobook_be.domain.notice.repository.NoticeRepository;
import com.example.egobook_be.domain.notification.service.NotificationService;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserStatus;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 공지사항 발행 시각 도달 시 전체 유저에게 NOTICE 타입 알림을 브로드캐스트
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeBroadcastScheduler {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 매분 정각마다 발행 시각이 지난 미발송 공지를 확인
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void broadcastDueNotices() {
        List<Notice> dueNotices = noticeRepository.findAllByPublishedAtLessThanEqualAndBroadcastedFalse(LocalDateTime.now());
        if (dueNotices.isEmpty()) {
            return;
        }

        List<User> activeUsers = userRepository.findAllByStatus(UserStatus.ACTIVE);
        for (Notice notice : dueNotices) {
            log.info("[NoticeBroadcastScheduler] 공지 브로드캐스트 시작 - noticeId: {}, 대상 유저 수: {}", notice.getId(), activeUsers.size());
            for (User user : activeUsers) {
                notificationService.createNoticeNotification(user, notice.getId(), notice.getTitle(), notice.getNotionUrl());
            }
            notice.markBroadcasted();
            log.info("[NoticeBroadcastScheduler] 공지 브로드캐스트 완료 - noticeId: {}", notice.getId());
        }
    }
}
