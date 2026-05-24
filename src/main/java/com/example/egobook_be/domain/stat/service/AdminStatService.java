package com.example.egobook_be.domain.stat.service;

import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.shop.repository.UserItemRepository;
import com.example.egobook_be.domain.stat.dto.*;
import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import com.example.egobook_be.domain.stat.exception.AdminStatErrorCode;
import com.example.egobook_be.domain.stat.mapper.AdminStatMapper;
import com.example.egobook_be.domain.stat.repository.AdminStatRepository;
import com.example.egobook_be.domain.stat.dto.AdminDauMauResDto;
import com.example.egobook_be.domain.stat.dto.AdminInkStatResDto;
import com.example.egobook_be.domain.stat.dto.AdminJoinWithdrawResDto;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserActivityLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.user.repository.WithdrawReasonRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatService {

    private final AdminStatRepository adminStatRepository;
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final PlazaLetterRepository plazaLetterRepository;
    private final InkLogRepository inkLogRepository;
    private final UserItemRepository userItemRepository;
    private final WithdrawReasonRepository withdrawReasonRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    /** 기간 별 DAU/MAU 통계 조회 */
    public AdminDauMauResDto getDauMau(LocalDate startDate, LocalDate endDate) {

        validDate(startDate, endDate);

        if (ChronoUnit.DAYS.between(startDate, endDate) > 90) {
            throw new CustomException(AdminStatErrorCode.DATE_RANGE_TOO_LONG);
        }

        Map<LocalDate, Long> dauMap = userActivityLogRepository
                .countDau(startDate, endDate).stream()
                .collect(Collectors.toMap(
                        UserActivityLogRepository.AuCount::getDate,
                        UserActivityLogRepository.AuCount::getCount
                ));

        Map<LocalDate, Long> mauMap = userActivityLogRepository
                .countMau(startDate, endDate).stream()
                .collect(Collectors.toMap(
                        UserActivityLogRepository.AuCount::getDate,
                        UserActivityLogRepository.AuCount::getCount
                ));

        List<AdminDauMauResDto.DauMauCount> data = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            data.add(AdminDauMauResDto.DauMauCount.builder()
                    .date(date)
                    .dau(dauMap.getOrDefault(date, 0L))
                    .mau(mauMap.getOrDefault(date, 0L))
                    .build());
        }

        return AdminDauMauResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .data(data)
                .build();
    }

    /** 기간/단위 별 신규 가입/탈퇴 통계 조회 */
    public AdminJoinWithdrawResDto getJoinWithdraw(LocalDate startDate, LocalDate endDate, AdminStatUnit unit) {

        validDate(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<AdminStatRepository.JoinWithdrawCount> join = adminStatRepository
                .countJoin(startDateTime, endDateTime, unit.getFormat());
        List<AdminStatRepository.JoinWithdrawCount> withdraw = adminStatRepository
                .countWithdraw(startDateTime, endDateTime, unit.getFormat());

        return AdminStatMapper.toJoinWithdrawResDto(startDate, endDate, unit, join, withdraw);
    }

    /** 7/30일 잔존율 조회 */
    public AdminRetentionResDto getRetention() {
        LocalDate today = LocalDate.now();

        LocalDateTime start7 = today.minusDays(7).atStartOfDay();
        LocalDateTime start30 = today.minusDays(30).atStartOfDay();

        Long total7 = userRepository.countByCreatedAtBefore(start7);
        Long total30 = userRepository.countByCreatedAtBefore(start30);

        Long active7 = userActivityLogRepository.countRetainedUserWithinDays(start7, 7);
        Long active30 = userActivityLogRepository.countRetainedUserWithinDays(start30, 30);

        return AdminStatMapper.getRetentionResDto(total7, total30, active7, active30);
    }

    /** 기간/타입별 일기 작성 수 조회 */
    public AdminDiaryStatResDto getDiaryTypeStat(LocalDate startDate, LocalDate endDate) {

        validDate(startDate, endDate);

        List<DiaryRepository.DiaryTypeCount> diaries = diaryRepository
                .countByTypeAndDateBetween(startDate, endDate);

        Long total = diaryRepository.countByDateBetween(startDate, endDate);

        return AdminStatMapper.getDiaryTypeStat(startDate, endDate, diaries, total);
    }

    /** 기간별 편지 답장 포기 통계 조회 */
    public AdminLetterGiveUpStatResDto getLetterGiveUpStat(LocalDate startDate, LocalDate endDate) {

        validDate(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Long total = plazaLetterRepository.countByCreatedAtGreaterThanEqualAndCreatedAtBefore(startDateTime, endDateTime);
        Long giveUp = plazaLetterRepository.countByGaveUpAtIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtBefore(startDateTime, endDateTime);

        return AdminStatMapper.getLetterGiveUpStatResDto(startDate, endDate, total, giveUp);
    }

    /** 기간/단위 별 잉크 발행/소비 통계 조회 */
    public AdminInkStatResDto getInkStat(LocalDate startDate, LocalDate endDate, AdminStatUnit unit) {

        validDate(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<InkLogRepository.InkStatAmount> issuedInk = inkLogRepository
                .sumIssued(startDateTime, endDateTime, unit.getFormat());

        List<InkLogRepository.InkStatAmount> consumedInkFromInkLog = inkLogRepository
                .sumConsumed(startDateTime, endDateTime, unit.getFormat());

        List<InkLogRepository.InkStatAmount> consumedInkFromUserItem = userItemRepository
                .sumConsumed(startDateTime, endDateTime, unit.getFormat());

        List<InkLogRepository.InkStatAmount> consumedInk = new ArrayList<>();

        consumedInk.addAll(consumedInkFromInkLog);
        consumedInk.addAll(consumedInkFromUserItem);

        return AdminStatMapper.toInkStatResDto(
                startDate,
                endDate,
                unit,
                issuedInk,
                consumedInk
        );
    }

    /** 기간 별 탈퇴 사유 통계 조회 */
    public AdminWithdrawReasonResDto getWithdrawReason(LocalDate startDate, LocalDate endDate) {

        validDate(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Long total = withdrawReasonRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtBefore(startDateTime, endDateTime);

        List<WithdrawReasonRepository.WithDrawReasonCount> reasons = withdrawReasonRepository
                .countReasons(startDateTime, endDateTime);

        List<String> otherTexts = withdrawReasonRepository.findOtherTexts(startDateTime, endDateTime);

        return AdminStatMapper.getWithdrawReasonResDto(
                startDate,
                endDate,
                total,
                reasons,
                otherTexts
        );
    }

    /** 날짜 범위 검증 공통 로직 */
    private void validDate (LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(AdminStatErrorCode.INVALID_STAT_DATE);
        }
    }
}