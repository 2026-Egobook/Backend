package com.example.egobook_be.domain.ego_room.dto;

public record MonthlyAvgDto(
        Integer year,
        Integer month,
        Double avg
) {}