package com.example.egobook_be.domain.notification.service;

import com.example.egobook_be.domain.letters.entity.PlazaLetter;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.notification.dto.NotificationReadResDto;
import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.exception.NotificationErrorCode;
import com.example.egobook_be.domain.notification.mapper.NotificationMapper;
import com.example.egobook_be.domain.notification.repository.NotificationRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PlazaLetterRepository plazaLetterRepository;

    /** 알림 생성 */
    @Transactional
    public void createNotification(Long userId, NotificationType type, Long targetId, String... args) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        String title = type.format(args);
        String content = switch (type) {
            case LETTER_REQUEST, FRIEND_LETTER -> getLetterPreview(targetId);
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
    }

    /** 알림 목록 */
    @Transactional(readOnly = true)
    public SliceResponse<NotificationResDto> getNotifications(Long userId, int page, int size) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Slice<Notification> slice = notificationRepository.findAllByUser(user, pageable);

        return SliceResponse.of(slice, NotificationMapper::toNotificationDto);
    }

    /** 알림 읽음 처리 */
    @Transactional
    public NotificationReadResDto readNotification(Long userId, Long notificationId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        notificationRepository.save(notification);

        return NotificationMapper.toNotificationReadDto(notification);
    }

    /** 알림 설정 확인 */
    @Transactional(readOnly = true)
    public boolean getNotificationSetting(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        return user.isNotificationEnabled();
    }

    /** 알림 설정 변경 */
    @Transactional
    public boolean updateNotificationSetting(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.USER_NOT_FOUND));

        user.updateNotificationEnabled();

        return user.isNotificationEnabled();
    }

    /** 편지 내용 미리보기 */
    private String getLetterPreview(Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        String content = letter.getContent();
        if (content == null) {
            return "";
        }

        return content.length() > 17
                ? content.substring(0, 17)
                : content;
    }
}
