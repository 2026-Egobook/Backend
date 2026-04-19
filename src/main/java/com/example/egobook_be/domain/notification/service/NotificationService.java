package com.example.egobook_be.domain.notification.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.entity.PlazaLetterReply;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.notification.dto.NotificationReadResDto;
import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.dto.NotificationSettingResDto;
import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.exception.NotificationErrorCode;
import com.example.egobook_be.domain.notification.mapper.NotificationMapper;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;

    private final FcmService fcmService;

    /** 알림 생성 */
    @Transactional
    public void createNotification(Long userId, NotificationType type, Long targetId, String... args) {
        log.info("[NotificationService] createNotification Start - userId: {}, targetId: {}", userId, targetId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        // 알림 설정 확인
        if (!user.isNotificationEnabled()) {
            return;
        }

        String title = type.format(args);
        String content = switch (type) {
            case LETTER_REPLY,
                 LETTER_REPLY_FRIEND,
                 LETTER_NEW,
                 LETTER_NEW_FRIEND
                    -> getLetterPreview(type, targetId);
            default -> null;
        };

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .targetId(targetId)
                .build();

        notificationRepository.save(notification);

        fcmService.sendPushNotification(user, notification);
        log.info("[NotificationService] createNotification End - userId: {}, targetId: {}", userId, targetId);
    }

    /** 알림 목록 */
    @Transactional(readOnly = true)
    public SliceResponse<NotificationResDto> getNotifications(Long userId, int page, int size) {
        log.info("[NotificationService] getNotifications Start - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        if (page < 1) {
            throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        }

        if (size < 1 || size > 100) {
            throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Slice<Notification> slice = notificationRepository.findAllByUser(user, pageable);

        log.info("[NotificationService] getNotifications End - userId: {}", userId);
        return SliceResponse.of(slice, NotificationMapper::toNotificationDto);
    }

    /** 알림 읽음 처리 */
    @Transactional
    public NotificationReadResDto readNotification(Long userId, Long notificationId) {
        log.info("[NotificationService] readNotification Start - userId: {}, notificationId: {}", userId, notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        notificationRepository.save(notification);

        log.info("[NotificationService] readNotification End - userId: {}, notificationId: {}", userId, notificationId);
        return NotificationMapper.toNotificationReadDto(notification);
    }

    /** 알림 설정 확인 */
    @Transactional(readOnly = true)
    public NotificationSettingResDto getNotificationSetting(Long userId) {
        log.info("[NotificationService] getNotificationSetting Start - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        log.info("[NotificationService] getNotificationSetting End - userId: {}", userId);
        return NotificationMapper.toNotificationSettingDto(user);
    }

    /** 알림 설정 변경 */
    @Transactional
    public NotificationSettingResDto updateNotificationSetting(Long userId) {
        log.info("[NotificationService] updateNotificationSetting Start - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        user.updateNotificationEnabled();

        log.info("[NotificationService] updateNotificationSetting Start - userId: {}", userId);
        return NotificationMapper.toNotificationSettingDto(user);
    }

    /** 편지 내용 미리보기 */
    private String getLetterPreview(NotificationType type, Long letterId) {

        String content;

        // Reply 타입인지 Letter 타입인지 구분
        if (type == NotificationType.LETTER_REPLY ||
                type == NotificationType.LETTER_REPLY_FRIEND) {
            PlazaLetterReply reply = plazaLetterReplyRepository.findById(letterId)
                    .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));
            content = reply.getContent();
        } else {
            PlazaLetter letter = plazaLetterRepository.findById(letterId)
                    .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));
            content = letter.getContent();
        }

        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 17
                ? content.substring(0, 17)
                : content;
    }
}
