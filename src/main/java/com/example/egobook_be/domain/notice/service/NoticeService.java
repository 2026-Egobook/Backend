package com.example.egobook_be.domain.notice.service;

import com.example.egobook_be.domain.notice.dto.NoticeLatestResDto;
import com.example.egobook_be.domain.notice.dto.NoticeRedDotResDto;
import com.example.egobook_be.domain.notice.entity.Notice;
import com.example.egobook_be.domain.notice.entity.NoticeRead;
import com.example.egobook_be.domain.notice.exception.NoticeErrorCode;
import com.example.egobook_be.domain.notice.repository.NoticeReadRepository;
import com.example.egobook_be.domain.notice.repository.NoticeRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
            markAsRead(user, notice);
            result = new NoticeLatestResDto(notice.getId(), notice.getTitle(), notice.getNotionUrl(), notice.getPublishedAt());
        }

        log.info("[NoticeService] getLatestNotice() - END | userId: {}, noticeId: {}", userId, notice == null ? null : notice.getId());
        return result;
    }

    /**
     * 공지사항 레드닷(안 읽은 공지 존재 여부)을 조회한다.
     * @param userId : 조회하는 유저 ID
     * @return : 레드닷 노출 여부
     */
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

    /**
     * 유저의 공지 읽음 여부를 기록한다. 이미 읽은 경우 별도 처리 없이 조용히 넘어간다.
     * @param user : 읽음 처리할 유저
     * @param notice : 읽음 처리할 공지
     */
    private void markAsRead(User user, Notice notice) {
        if (noticeReadRepository.existsByUserAndNotice(user, notice)) {
            return;
        }
        try {
            noticeReadRepository.save(NoticeRead.builder().user(user).notice(notice).build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 이미 읽음 처리된 경우 - 무시
            log.warn("[NoticeService] markAsRead() - 동시 요청으로 인한 중복 저장 시도, userId: {}, noticeId: {}", user.getId(), notice.getId());
        }
    }
}
