package com.example.egobook_be.domain.notification.repository;

import com.example.egobook_be.domain.notification.entity.Notification;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findAllByUser(User user, Pageable pageable);

    /** 해당 사용자가 아직 읽지 않은 알림의 개수를 반환하는 함수 */
    Integer countByUserAndIsReadIsFalse(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}
