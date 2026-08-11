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

    /**
     * 개인 대상 쿠폰의 코드가 담긴 알림을 생성한다.
     * - 오늘의 질문 답변 선정 보상으로 지급되는 쿠폰이며, 제목은 고정이고 content에 쿠폰 코드를 싣는다.
     * @param user : 알림을 받을 유저
     * @param couponId : 대상 쿠폰 PK
     * @param code : 쿠폰 코드
     * @return : 알림이 생성되었으면 true, 유저가 알림을 꺼두어 생성하지 않았으면 false
     */
    @Transactional
    public boolean createCouponNotification(User user, Long couponId, String code) {
        if (!user.isNotificationEnabled()) {
            log.warn("[NotificationService] 알림 설정 꺼짐으로 쿠폰 알림 미생성 - userId: {}", user.getId());
            return false;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.COUPON)
                .title(NotificationType.COUPON.getTitle())
                .content("쿠폰코드 : " + code)
                .targetId(couponId)
                .build();

        notificationRepository.save(notification);
        fcmService.sendPushNotification(user, notification);
        return true;
    }

    /** 알림 목록 (공지사항은 /notices API로 분리되어 있음) */
    @Transactional
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

        // 응답에 담길 스냅샷은 갱신 전 상태를 기준으로 조회 (목록 내 개별 읽음 표시는 isRead로 그대로 유지)
        Slice<Notification> slice = notificationRepository.findAllByUser(user, pageable);

        // 알림 목록을 연 시점을 기록 - 레드닷은 이 시각 이후 새 알림이 있는지로 판단 (isRead와 무관)
        user.updateLastNotificationCheckedAt();

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

        // 목록 화면을 거치지 않고 푸시로 직접 확인한 경우에도 레드닷 기준 시각 갱신
        notification.getUser().updateLastNotificationCheckedAt();

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
