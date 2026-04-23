package com.example.egobook_be.domain.stat.dto;

import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminWithdrawReasonResDto(
        LocalDate startDate,
        LocalDate endDate,
        Long total,
        List<WithdrawReasonStat> data
) {
    @Builder
    public record WithdrawReasonStat (
            WithdrawReasonType reason,
            Long count,
            Double ratio,
            List<String> text
    ) {}
}
