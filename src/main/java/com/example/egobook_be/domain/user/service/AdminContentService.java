package com.example.egobook_be.domain.user.service;

import com.example.egobook_be.domain.ego_room.entity.DailyPraiseSendFailLog;
import com.example.egobook_be.domain.ego_room.entity.WeeklyReportSendFailLog;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyReportSendFailLogRepository;
import com.example.egobook_be.domain.ego_room.service.EgoRoomService;
import com.example.egobook_be.domain.letters.entity.BadWordBlockLog;
import com.example.egobook_be.domain.letters.entity.LetterSendFailLog;
import com.example.egobook_be.domain.letters.entity.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.enums.BlockType;
import com.example.egobook_be.domain.letters.repository.AiRequestCountLogRepository;
import com.example.egobook_be.domain.letters.repository.BadWordBlockLogRepository;
import com.example.egobook_be.domain.letters.repository.LetterSendFailLogRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.dto.ResendReqDto;
import com.example.egobook_be.domain.user.dto.AdminContentResDto.*;
import com.example.egobook_be.domain.user.exception.AdminContentErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final DailyPraiseSendFailLogRepository dailyPraiseFailLogRepo;
    private final WeeklyReportSendFailLogRepository weeklyReportFailLogRepo;
    private final LetterSendFailLogRepository letterFailLogRepo;
    private final BadWordBlockLogRepository badWordBlockLogRepo;
    private final DailyPraiseRepository dailyPraiseRepo;
    private final WeeklyCounselRepository weeklyCounselRepo;
    private final PlazaLetterRepository plazaLetterRepo;
    private final UserRepository userRepository;
    private final EgoRoomService egoRoomService;
    private final AiRequestCountLogRepository aiRequestCountLogRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // B1. AI 일간 칭찬서
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DailyPraiseStatusRes getDailyPraiseStatus(LocalDate startDate, LocalDate endDate) {
        log.info("[AdminContentService] getDailyPraiseStatus Start - startDate: {}, endDate: {}", startDate, endDate);
        validateDateRange(startDate, endDate);

        List<DailyPraiseSendFailLog> failLogs =
                dailyPraiseFailLogRepo.findByTargetDateBetweenOrderByFailedAtDesc(startDate, endDate);

        // 날짜별 성공 건수 (DailyPraise 생성 = 발송 성공)
        List<Object[]> successByDate = dailyPraiseRepo.countSuccessByDateRange(startDate, endDate);
        // 날짜별 실패 건수
        List<Object[]> failByDate = dailyPraiseFailLogRepo.countByDateRange(startDate, endDate);

        Map<LocalDate, Long> successMap = toDateCountMap(successByDate);
        Map<LocalDate, Long> failMap = toDateCountMap(failByDate);

        // scheduledCount = dailyPraise=true 유저 수 (현재 기준)
        long scheduledPerDay = userRepository.findByDailyPraiseTrue().size();
        List<LocalDate> dateRange = buildDateRange(startDate, endDate);

        List<DailyStat> dailyStats = dateRange.stream()
                .map(date -> DailyStat.builder()
                        .date(date)
                        .scheduledCount(scheduledPerDay)
                        .successCount(successMap.getOrDefault(date, 0L))
                        .failCount(failMap.getOrDefault(date, 0L))
                        .build())
                .collect(Collectors.toList());

        long totalSuccess = successMap.values().stream().mapToLong(Long::longValue).sum();

        SummaryWithDaily summary = SummaryWithDaily.builder()
                .scheduledCount(scheduledPerDay * dateRange.size())
                .successCount(totalSuccess)
                .failCount(failLogs.size())
                .build();

        List<PraiseFailLog> failLogDtos = failLogs.stream()
                .map(f -> PraiseFailLog.builder()
                        .failId(f.getId())
                        .userId(f.getUserId())
                        .failedAt(f.getFailedAt())
                        .reason(f.getReason())
                        .build())
                .collect(Collectors.toList());

        log.info("[AdminContentService] getDailyPraiseStatus End - startDate: {}, endDate: {}", startDate, endDate);
        return DailyPraiseStatusRes.builder()
                .summary(summary)
                .dailyStats(dailyStats)
                .failLogs(failLogDtos)
                .build();
    }

    @Transactional
    public ResendRes resendDailyPraise(ResendReqDto reqDto) {
        log.info("[AdminContentService] resendDailyPraise Start");
        validateFailIds(reqDto.getFailIds());

        List<ResendResult> results = new ArrayList<>();
        long successCount = 0, failCount = 0;

        for (Long failId : reqDto.getFailIds()) {
            Optional<DailyPraiseSendFailLog> opt = dailyPraiseFailLogRepo.findById(failId);

            if (opt.isEmpty()) {
                results.add(failResult(failId, "NOT_FOUND"));
                failCount++;
                continue;
            }

            DailyPraiseSendFailLog failLog = opt.get();
            if (failLog.isResent()) {
                results.add(failResult(failId, "ALREADY_SENT"));
                failCount++;
                continue;
            }

            try {
                // createDailyPraise와 로직은 동일하되 재발송 전용 메서드 호출 (중복 체크 없이 강제 재생성)
                egoRoomService.resendDailyPraise(failLog.getUserId(), failLog.getTargetDate());
                failLog.markResent();
                results.add(successResult(failId));
                successCount++;
            } catch (Exception e) {
                log.error("[AdminContent] 일간 칭찬서 재발송 실패 failId={}: {}", failId, e.getMessage());
                results.add(failResult(failId, "RESEND_FAILED"));
                failCount++;
            }
        }
        log.info("[AdminContentService] resendDailyPraise End");
        return ResendRes.builder()
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B2. AI 주간 리포트
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WeeklyReportStatusRes getWeeklyReportStatus(LocalDate startDate, LocalDate endDate) {
        log.info("[AdminContentService] getWeeklyReportStatus Start - startDate: {}. endDate: {}", startDate, endDate);
        validateDateRange(startDate, endDate);

        List<WeeklyReportSendFailLog> failLogs =
                weeklyReportFailLogRepo.findByWeekStartDateBetweenOrderByFailedAtDesc(startDate, endDate);

        long successCount = weeklyCounselRepo.countByStartDateBetween(startDate, endDate);
        long scheduledCount = userRepository.findAllByWeeklyAnalysisEnabledTrue().size();

        SimpleSummary summary = SimpleSummary.builder()
                .scheduledCount(scheduledCount)
                .successCount(successCount)
                .failCount(failLogs.size())
                .build();

        List<WeeklyFailLog> failLogDtos = failLogs.stream()
                .map(f -> WeeklyFailLog.builder()
                        .failId(f.getId())
                        .userId(f.getUserId())
                        .failedAt(f.getFailedAt())
                        .reason(f.getReason())
                        .build())
                .collect(Collectors.toList());

        log.info("[AdminContentService] getWeeklyReportStatus End - startDate: {}. endDate: {}", startDate, endDate);
        return WeeklyReportStatusRes.builder()
                .summary(summary)
                .failLogs(failLogDtos)
                .build();
    }

    @Transactional
    public ResendRes resendWeeklyReport(ResendReqDto reqDto) {
        log.info("[AdminContentService] resendWeeklyReport Start");
        validateFailIds(reqDto.getFailIds());

        List<ResendResult> results = new ArrayList<>();
        long successCount = 0, failCount = 0;

        for (Long failId : reqDto.getFailIds()) {
            Optional<WeeklyReportSendFailLog> opt = weeklyReportFailLogRepo.findById(failId);

            if (opt.isEmpty()) {
                results.add(failResult(failId, "NOT_FOUND"));
                failCount++;
                continue;
            }

            WeeklyReportSendFailLog failLog = opt.get();
            if (failLog.isResent()) {
                results.add(failResult(failId, "ALREADY_SENT"));
                failCount++;
                continue;
            }

            try {
                // createWeeklyAnalysis와 로직은 동일하되 재발송 전용 메서드 호출 (중복 체크 없이 강제 재생성)
                egoRoomService.resendWeeklyAnalysis(failLog.getUserId(), failLog.getWeekStartDate());
                failLog.markResent();
                results.add(successResult(failId));
                successCount++;
            } catch (Exception e) {
                log.error("[AdminContent] 주간 리포트 재발송 실패 failId={}: {}", failId, e.getMessage());
                results.add(failResult(failId, "RESEND_FAILED"));
                failCount++;
            }
        }

        log.info("[AdminContentService] resendWeeklyReport End");
        return ResendRes.builder()
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B3. 편지
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LetterStatusRes getLetterStatus(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long sentCount = plazaLetterRepo.countByCreatedAtBetweenAndStatuses(
                start, end, List.of(PlazaLetterStatus.REPLIED, PlazaLetterStatus.AI_REPLIED));

        long waitingCount = plazaLetterRepo.countByStatus(PlazaLetterStatus.WAITING);

        long aiReplyCount = plazaLetterRepo.countByCreatedAtBetweenAndStatus(
                start, end, PlazaLetterStatus.AI_REPLIED);

        List<LetterSendFailLog> failLogs =
                letterFailLogRepo.findByFailedAtBetweenOrderByFailedAtDesc(start, end);

        LetterSummary summary = LetterSummary.builder()
                .sentCount(sentCount)
                .waitingCount(waitingCount)
                .aiReplyCount(aiReplyCount)
                .failCount(failLogs.size())
                .build();

        List<LetterFailLog> failLogDtos = failLogs.stream()
                .map(f -> LetterFailLog.builder()
                        .logId(f.getId())
                        .letterId(f.getLetterId())
                        .failedAt(f.getFailedAt())
                        .reason(f.getReason())
                        .build())
                .collect(Collectors.toList());

        return LetterStatusRes.builder()
                .summary(summary)
                .failLogs(failLogDtos)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B4. 나쁜말 AI 차단
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BadWordStatusRes getBadWordStatus(LocalDate startDate, LocalDate endDate, BlockType type) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<BadWordBlockLog> blockedLogs = (type == null || type == BlockType.ALL)
                ? badWordBlockLogRepo.findByBlockedAtBetweenOrderByBlockedAtDesc(start, end)
                : badWordBlockLogRepo.findByBlockedAtBetweenAndTypeOrderByBlockedAtDesc(start, end, type);

        // AiRequestCountLog 기반 전체 요청 수로 차단율 계산
        long totalCount = (type == null || type == BlockType.ALL)
                ? aiRequestCountLogRepo.countByRequestedAtBetween(start, end)
                : aiRequestCountLogRepo.countByRequestedAtBetweenAndType(start, end, type);

        double blockRate = totalCount == 0 ? 0.0
                : Math.round((double) blockedLogs.size() / totalCount * 1000) / 10.0; // 소수점 1자리 %

        BadWordSummary summary = BadWordSummary.builder()
                .blockedCount(blockedLogs.size())
                .blockRate(blockRate)
                .build();

        List<BlockedLog> logDtos = blockedLogs.stream()
                .map(b -> BlockedLog.builder()
                        .blockId(b.getId())
                        .userId(b.getUserId())
                        .type(b.getType().name())
                        .originalText(b.getOriginalText())
                        .badWords(b.getBadWords())
                        .score(b.getScore())
                        .blockedAt(b.getBlockedAt())
                        .build())
                .collect(Collectors.toList());

        return BadWordStatusRes.builder()
                .summary(summary)
                .blockedLogs(logDtos)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 유틸
    // ─────────────────────────────────────────────────────────────────────────

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(AdminContentErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateFailIds(List<Long> failIds) {
        if (failIds == null || failIds.isEmpty()) {
            throw new CustomException(AdminContentErrorCode.FAIL_IDS_EMPTY);
        }
    }

    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private Map<LocalDate, Long> toDateCountMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }

    private ResendResult successResult(Long failId) {
        return ResendResult.builder().failId(failId).status("SUCCESS").build();
    }

    private ResendResult failResult(Long failId, String reason) {
        return ResendResult.builder().failId(failId).status("FAIL").reason(reason).build();
    }
}