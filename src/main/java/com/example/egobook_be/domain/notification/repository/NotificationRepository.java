package com.example.egobook_be.domain.notification.repository;

import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findAllByUser(User user, Pageable pageable);
}
