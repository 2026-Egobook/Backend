package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
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

    Optional<WeeklyCounsel> findByUserIdAndStartDate(Long userId, LocalDate startDate);

    Slice<WeeklyCounsel> findAllByUserId(Long userId, Pageable pageable);
}