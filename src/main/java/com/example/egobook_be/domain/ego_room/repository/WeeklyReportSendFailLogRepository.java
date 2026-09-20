package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.WeeklyReportSendFailLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportSendFailLogRepository extends JpaRepository<WeeklyReportSendFailLog, Long> {

    List<WeeklyReportSendFailLog> findByWeekStartDateBetweenOrderByFailedAtDesc(
            LocalDate startDate, LocalDate endDate
    );

    List<WeeklyReportSendFailLog> findByWeekStartDateBetweenAndResentFalseOrderByFailedAtDesc(
            LocalDate startDate, LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM WeeklyReportSendFailLog f WHERE f.id = :failId")
    Optional<WeeklyReportSendFailLog> findByIdWithLock(@Param("failId") Long failId);
}