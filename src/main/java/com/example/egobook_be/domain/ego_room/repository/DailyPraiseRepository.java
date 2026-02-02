package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPraiseRepository extends JpaRepository<DailyPraise, Long> {

    Slice<DailyPraise> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT dp FROM DailyPraise dp " +
            "JOIN FETCH dp.diary d " +
            "WHERE d.user.id = :userId " +
            "AND dp.praiseDate = :date")
    Optional<DailyPraise> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );


}