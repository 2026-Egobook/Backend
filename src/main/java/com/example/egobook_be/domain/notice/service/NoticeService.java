package com.example.egobook_be.domain.notice.service;

import com.example.egobook_be.domain.notice.dto.NoticeLatestResDto;
import com.example.egobook_be.domain.notice.dto.NoticeRedDotResDto;
import com.example.egobook_be.domain.notice.entity.Notice;
import com.example.egobook_be.domain.notice.exception.NoticeErrorCode;
import com.example.egobook_be.domain.notice.repository.NoticeReadRepository;
import com.example.egobook_be.domain.notice.repository.NoticeRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final UserRepository userRepository;


    @Transactional
    public NoticeLatestResDto getLatestNotice(Long userId) {
        log.info("[NoticeService] getLatestNotice() - START | userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NoticeErrorCode.USER_NOT_FOUND));

        Notice notice = noticeRepository
                .findFirstByPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime.now())
                .orElse(null);

        NoticeLatestResDto result = null;
        if (notice != null) {
            noticeReadRepository.insertIfNotExists(user.getId(), notice.getId());
            result = new NoticeLatestResDto(notice.getId(), notice.getTitle(), notice.getNotionUrl(), notice.getPublishedAt());
        }

        log.info("[NoticeService] getLatestNotice() - END | userId: {}, noticeId: {}", userId, notice == null ? null : notice.getId());
        return result;
    }


    @Transactional(readOnly = true)
    public NoticeRedDotResDto getRedDotStatus(Long userId) {
        log.info("[NoticeService] getRedDotStatus() - START | userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NoticeErrorCode.USER_NOT_FOUND));

        Notice notice = noticeRepository
                .findFirstByPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime.now())
                .orElse(null);

        boolean hasUnreadNotice = notice != null && !noticeReadRepository.existsByUserAndNotice(user, notice);
        NoticeRedDotResDto result = new NoticeRedDotResDto(hasUnreadNotice);

        log.info("[NoticeService] getRedDotStatus() - END | userId: {}, hasUnreadNotice: {}", userId, hasUnreadNotice);
        return result;
    }
}
