package com.example.egobook_be.domain.user.repository;

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
}