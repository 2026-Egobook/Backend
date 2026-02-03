package com.example.egobook_be.domain.ego_room.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyCounselRepository extends JpaRepository<WeeklyCounsel, Long> {

    @Query("SELECT wc FROM WeeklyCounsel wc " +
            "WHERE wc.user.id = :userId " +
            "AND (:cursor = 1L OR wc.id < :cursor) " +
            "ORDER BY wc.id DESC")
    Slice<WeeklyCounsel> findWeeklyCounselList(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    boolean existsByUserIdAndStartDate(Long userId, LocalDate startDate);

    Optional<WeeklyCounsel> findByUserIdAndStartDate(Long userId, LocalDate startDate);

    Slice<WeeklyCounsel> findAllByUserId(Long userId, Pageable pageable);

    Optional<WeeklyCounsel> findTopByUserOrderByEndDateDesc(User user);
}