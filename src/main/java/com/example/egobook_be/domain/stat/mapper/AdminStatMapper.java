package com.example.egobook_be.domain.stat.mapper;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.stat.dto.*;
import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import com.example.egobook_be.domain.stat.repository.AdminStatRepository;
import com.example.egobook_be.domain.stat.dto.AdminDauMauResDto;
import com.example.egobook_be.domain.stat.dto.AdminInkStatResDto;
import com.example.egobook_be.domain.stat.dto.AdminJoinWithdrawResDto;
import com.example.egobook_be.domain.user.enums.WithdrawReasonType;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.WithdrawReasonRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class AdminStatMapper {

    public static AdminJoinWithdrawResDto toJoinWithdrawResDto(
            LocalDate startDate,
            LocalDate endDate,
            AdminStatUnit unit,
            List<AdminStatRepository.JoinWithdrawCount> joins,
            List<AdminStatRepository.JoinWithdrawCount> withdraws
    ) {
        Map<String, Long> joinMap = joins.stream()
                .collect(Collectors.toMap(
                        AdminStatRepository.JoinWithdrawCount::getPeriod,
                        AdminStatRepository.JoinWithdrawCount::getCount
                ));

        Map<String, Long> withdrawalMap = withdraws.stream()
                .collect(Collectors.toMap(
                        AdminStatRepository.JoinWithdrawCount::getPeriod,
                        AdminStatRepository.JoinWithdrawCount::getCount
                ));

        List<String> periods = generatePeriods(startDate, endDate, unit);

        List<AdminJoinWithdrawResDto.JoinWithdraw> data = periods.stream()
                .map(period-> {
                    long join = joinMap.getOrDefault(period, 0L);
                    long withdraw = withdrawalMap.getOrDefault(period, 0L);
                    return AdminJoinWithdrawResDto.JoinWithdraw.builder()
                            .period(period)
                            .join(join)
                            .withdraw(withdraw)
                            .netChange(join - withdraw)
                            .build();
                }).toList();

        return AdminJoinWithdrawResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .unit(unit)
                .data(data)
                .build();
    }

    public static AdminDiaryStatResDto getDiaryTypeStat(
            LocalDate startDate,
            LocalDate endDate,
            List<DiaryRepository.DiaryTypeCount> diaryTypeCounts,
            Long totalCount
    ) {

        Map<DiaryType, Long> countMap = diaryTypeCounts.stream()
                .collect(Collectors.toMap(
                        DiaryRepository.DiaryTypeCount::getType,
                        DiaryRepository.DiaryTypeCount::getCount
                ));

        List<AdminDiaryStatResDto.DiaryTypeCount> data = Arrays.stream(DiaryType.values())
                .map(type -> AdminDiaryStatResDto.DiaryTypeCount.builder()
                        .type(type)
                        .count(countMap.getOrDefault(type, 0L))
                        .build())
                .toList();

        return AdminDiaryStatResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .total(totalCount)
                .data(data)
                .build();
    }

    public static AdminLetterGiveUpStatResDto getLetterGiveUpStatResDto(
            LocalDate startDate,
            LocalDate endDate,
            Long totalCount,
            Long gaveUpCount
    ) {
        Double gaveUpRate = totalCount == 0 ? 0.0
                : Math.round((double) gaveUpCount / totalCount * 10_000.0) / 100.0;

        return AdminLetterGiveUpStatResDto.builder()
                .starDate(startDate)
                .endDate(endDate)
                .total(totalCount)
                .giveUp(gaveUpCount)
                .giveUpRate(gaveUpRate)
                .build();
    }

    public static AdminInkStatResDto toInkStatResDto(
            LocalDate startDate,
            LocalDate endDate,
            AdminStatUnit unit,
            List<InkLogRepository.InkStatAmount> issuedInk,
            List<InkLogRepository.InkStatAmount> consumedInk
    ) {

        Map<String, Long> issuedMap = issuedInk.stream()
                .collect(Collectors.toMap(
                        InkLogRepository.InkStatAmount::getPeriod,
                        InkLogRepository.InkStatAmount::getAmount
                ));


        Map<String, Long> consumedMap = consumedInk.stream()
                .collect(Collectors.groupingBy(
                        InkLogRepository.InkStatAmount::getPeriod,
                        Collectors.summingLong(InkLogRepository.InkStatAmount::getAmount)
                ));

        List<String> periods = generatePeriods(startDate, endDate, unit);

        List<AdminInkStatResDto.InkStat> data = periods.stream()
                .map(period -> {
                    Long issued   = issuedMap.getOrDefault(period, 0L);
                    Long consumed = consumedMap.getOrDefault(period, 0L);
                    return AdminInkStatResDto.InkStat.builder()
                            .period(period)
                            .issued(issued)
                            .consumed(consumed)
                            .netChange(issued - consumed)
                            .build();
                })
                .toList();

        return AdminInkStatResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .unit(unit)
                .data(data)
                .build();
    }

    public static AdminWithdrawReasonResDto getWithdrawReasonResDto(
            LocalDate startDate,
            LocalDate endDate,
            Long total,
            List<WithdrawReasonRepository.WithDrawReasonCount> reasonCount,
            List<String> otherTexts
    ) {

        Map<WithdrawReasonType, Long> reasonMap = reasonCount.stream()
                .collect(Collectors.toMap(
                        WithdrawReasonRepository.WithDrawReasonCount::getType,
                        WithdrawReasonRepository.WithDrawReasonCount::getCount
                ));

        List<AdminWithdrawReasonResDto.WithdrawReasonStat> data = Arrays.stream(WithdrawReasonType.values())
                .map(reason-> {
                    Long count = reasonMap.getOrDefault(reason, 0L);
                    Double ratio = total == 0 ? 0.0
                            : Math.round((double) count / total * 1_000.0) / 1_000.0;
                    return AdminWithdrawReasonResDto.WithdrawReasonStat.builder()
                            .reason(reason)
                            .count(count)
                            .ratio(ratio)
                            .text(reason == WithdrawReasonType.OTHER ? otherTexts : null)
                            .build();
                }).toList();

        return AdminWithdrawReasonResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .total(total)
                .data(data)
                .build();
    }

    private static List<String> generatePeriods(LocalDate startDate, LocalDate endDate, AdminStatUnit unit) {
        List<String> periods = new ArrayList<>();

        if (unit == AdminStatUnit.MONTH) {
            YearMonth current = YearMonth.from(startDate);
            YearMonth end = YearMonth.from(endDate);

            while (!current.isAfter(end)) {
                periods.add(current.toString());
                current = current.plusMonths(1);
            }

        } else { // WEEK
            WeekFields wf = WeekFields.ISO;
            LocalDate current = startDate;

            while (!current.isAfter(endDate)) {
                int year = current.get(wf.weekBasedYear());
                int week = current.get(wf.weekOfWeekBasedYear());
                String period = String.format("%d-W%02d", year, week);

                if (!periods.contains(period)) {
                    periods.add(period);
                }

                current = current.plusDays(1);
            }
        }

        return periods;
    }

    public static AdminRetentionResDto getRetentionResDto(Long total7, Long total30, Long active7, Long active30) {

        return AdminRetentionResDto.builder()
                .day7RetentionRate(calcRate(active7, total7))
                .day30RetentionRate(calcRate(active30, total30))
                .build();
    }

    private static double calcRate(Long active, Long total) {
        if (total == null || total == 0) return 0.0;
        return Math.round((active * 100.0 / total) * 10) / 10.0;
    }
}
