package com.example.egobook_be.domain.stat.dto;

import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminJoinWithdrawResDto(
        LocalDate startDate,
        LocalDate endDate,
        AdminStatUnit unit,
        List<JoinWithdraw> data
) {

    @Builder
    public record JoinWithdraw(
            String period,
            Long join,
            Long withdraw,
            Long netChange
    ) {}
}
