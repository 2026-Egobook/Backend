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

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findAllByUser(User user, Pageable pageable);

    /** 해당 사용자가 아직 읽지 않은 알림의 개수를 반환하는 함수 */
    Integer countByUserAndIsReadIsFalse(User user);

    // 마지막 알림 확인 시각 이후 새로 생성된 알림 개수 (레드닷 판단 기준, isRead와 무관)
    /**
     * 마지막으로 알림을 확인한 시각(checkedAt) 이후 생성된 알림 개수를 반환한다.
     * @param user : 조회 대상 유저
     * @param checkedAt : 마지막 알림 확인 시각
     * @return : checkedAt 이후 생성된 알림 개수
     */
    Integer countByUserAndCreatedAtAfter(User user, LocalDateTime checkedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}
