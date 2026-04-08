package com.example.egobook_be.domain.ego_room.repository;

import com.example.egobook_be.domain.ego_room.entity.WeeklyReportSendFailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyReportSendFailLogRepository extends JpaRepository<WeeklyReportSendFailLog, Long> {

    List<WeeklyReportSendFailLog> findByWeekStartDateBetweenOrderByFailedAtDesc(
            LocalDate startDate, LocalDate endDate
    );
}