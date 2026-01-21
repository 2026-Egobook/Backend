package com.example.egobook_be.domain.notification.service;

import com.example.egobook_be.domain.notification.dto.NotificationResDto;
import com.example.egobook_be.domain.notification.entity.Notification;
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
}
