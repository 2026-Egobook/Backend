package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.TodayQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TodayQuestionRepository extends JpaRepository<TodayQuestion, Long> {
    Optional<TodayQuestion> findByQuestionDate(LocalDate date);
}
