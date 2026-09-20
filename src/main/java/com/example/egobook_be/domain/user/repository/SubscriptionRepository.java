package com.example.egobook_be.domain.user.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.user.id = :userId " +
            "AND s.status = 'ACTIVE' " +
            "AND s.expirationDate >= :today")
    Optional<Subscription> findActiveSubscription(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Subscription x WHERE x.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}
