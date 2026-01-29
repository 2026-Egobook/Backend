package com.example.egobook_be.domain.psychology.dto;

public record RewardInfoResDto(
        boolean granted,
        Integer inkGranted,
        Integer inkBalance,
        String toastMessage
) {}