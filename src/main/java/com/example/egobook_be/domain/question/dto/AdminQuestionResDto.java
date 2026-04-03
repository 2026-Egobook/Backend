package com.example.egobook_be.domain.question.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminQuestionResDto(Long id, String content, LocalDate questionDate, LocalDateTime createdAt,LocalDateTime deletedAt) {}

