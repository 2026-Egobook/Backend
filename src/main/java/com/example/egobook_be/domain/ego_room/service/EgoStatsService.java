package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class EgoStatsService {

    private final DiaryRepository diaryRepository;
    // private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public EgoStatsResDto getMonthlyStats(Long userId, int year, int month) {
        log.info("유저 {}번의 {}년 {}월 통계 조회 시작", userId, year, month);

        /* 구독 체크
        subscriptionRepository.findActiveSubscription(userId, LocalDate.now())
                .orElseThrow(() -> new SubscriptionLockedException(
                        EgoRoomErrorCode.WEEKLY_COUNSEL_LOCKED,
                        "SUB403_STATS_LOCKED",
                        "B1.3_SETTINGS"
                ));
        */

        //광고보는 로직

        LocalDate targetDate = LocalDate.of(year, month, 1);
        LocalDateTime endOfPeriod = targetDate.withDayOfMonth(targetDate.lengthOfMonth()).atTime(23, 59, 59);

        // 시작 지점 계산 (이번 달 포함 12개월, 6개월)
        LocalDateTime startOfOneYear = targetDate.minusMonths(11).atStartOfDay();
        LocalDateTime startOfSixMonths = targetDate.minusMonths(5).atStartOfDay();

        // 1년치 일기 데이터 조회 및 필터링
        List<Diary> yearlyDiaries = diaryRepository.findAllByUserIdAndWrittenAtAfter(userId, startOfOneYear)
                .stream()
                .filter(d -> d.getEmotionLevel() > 0)
                .filter(d -> !d.getWrittenAt().isAfter(endOfPeriod))
                .toList();


        if (yearlyDiaries.isEmpty()) {
            int startYear = startOfOneYear.getYear();
            int startMonth = startOfOneYear.getMonthValue();
            return EgoStatsResDto.empty(year, month, startYear, startMonth);
        }

        // 워드클라우드용- 최근 6개월치만 따로 필터링
        List<Diary> sixMonthDiaries = yearlyDiaries.stream()
                .filter(d -> d.getWrittenAt().isAfter(startOfSixMonths))
                .toList();

        // 요일별 감정 스택 통계 계산
        StackedStatsDto stacked = calculateStackedStats(yearlyDiaries);

        // 6개월 평균 감정 점수 계산
        List<MonthlyAvgDto> sixMonthAvgs = calculateSixMonthAvgs(userId, year, month);

        // 6개월 워드 클라우드
        List<WordCloudDto> wordCloud = calculateWordCloud(sixMonthDiaries);

        // 전체 카운트 계산
        TotalStatsDto totalCounts = calculateTotalCounts(yearlyDiaries);

        MoodPeakResDto moodPeak = calculateMoodPeak(yearlyDiaries);

        return EgoStatsResDto.builder()
                .startYear(startOfOneYear.getYear())
                .startMonth(startOfOneYear.getMonthValue())
                .year(year)
                .month(month)
                .totalStats(totalCounts)
                .moodPeak(moodPeak)
                .stacked(stacked)
                .sixMonthAvgs(sixMonthAvgs)
                .wordCloud(wordCloud)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private TotalStatsDto calculateTotalCounts(List<Diary> diaries) {
        Map<Integer, Long> countsMap = diaries.stream() .collect(Collectors.groupingBy(Diary::getEmotionLevel, Collectors.counting()));

        List<TotalCountDto> counts = IntStream.rangeClosed(1, 5)
                .mapToObj(lvl -> new TotalCountDto(lvl, countsMap.getOrDefault(lvl, 0L).intValue()))
                .toList();

        int maxCount = counts.stream()
                .mapToInt(TotalCountDto::totalCount)
                .max()
                .orElse(0);

        return new TotalStatsDto(maxCount, counts);
    }

    private MoodPeakResDto calculateMoodPeak(List<Diary> diaries) {
        List<Diary> goodDiaries = diaries.stream().filter(d -> d.getEmotionLevel() >= 3).toList();
        List<Diary> badDiaries = diaries.stream().filter(d -> d.getEmotionLevel() >= 1 && d.getEmotionLevel() <= 2).toList();

        return new MoodPeakResDto(findPeak(goodDiaries), findPeak(badDiaries));
    }

    private PeakDetailDto findPeak(List<Diary> diaries) {
        if (diaries.isEmpty()) return new PeakDetailDto(null, 0);

        String peakDay = diaries.stream()
                .collect(Collectors.groupingBy(d -> d.getWrittenAt().getDayOfWeek(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name().substring(0, 3))
                .orElse(null);

        int peakHour = diaries.stream()
                .collect(Collectors.groupingBy(d -> d.getWrittenAt().getHour(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        return new PeakDetailDto(peakDay, peakHour);
    }

    private StackedStatsDto calculateStackedStats(List<Diary> diaries) {
        List<WeekdayStackDto> byWeekday = Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    List<Diary> dayDiaries = diaries.stream()
                            .filter(d -> d.getWrittenAt().getDayOfWeek() == day)
                            .toList();
                    int total = dayDiaries.size();

                    List<LevelDto> levels = IntStream.rangeClosed(1, 5)
                            .mapToObj(lvl -> {
                                long count = dayDiaries.stream().filter(d -> d.getEmotionLevel() == lvl).count();
                                int percent = total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
                                return new LevelDto(lvl, percent);
                            }).toList();
                    return new WeekdayStackDto(day.name().substring(0, 3), levels);
                }).toList();

        return new StackedStatsDto(byWeekday);
    }

    private List<MonthlyAvgDto> calculateSixMonthAvgs(Long userId, int year, int month) {
        // 현재 월부터 직전 5개월까지 총 6개월치 데이터 계산
        return IntStream.range(0, 6)
                .mapToObj(i -> {
                    LocalDate targetDate = LocalDate.of(year, month, 1).minusMonths(i);
                    LocalDateTime start = targetDate.atStartOfDay();
                    LocalDateTime end = targetDate.withDayOfMonth(targetDate.lengthOfMonth()).atTime(23, 59, 59);

                    Double avg = diaryRepository.findAvgEmotionLevel(userId, start, end);
                    return new MonthlyAvgDto(targetDate.getYear(), targetDate.getMonthValue(), avg != null ? avg : 0.0);
                })
                .sorted(Comparator.comparing(MonthlyAvgDto::year).thenComparing(MonthlyAvgDto::month))
                .toList();
    }

    private List<WordCloudDto> calculateWordCloud(List<Diary> diaries) {
        // 모든 일기의 본문을 하나로 합치고 특수문자 제거
        // 한글, 영어, 숫자만 남기고 나머지는 공백으로 치환
        String allContent = diaries.stream()
                .map(Diary::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");

        // 공백 기준으로 단어 분리 후 빈도수 계산
        Map<String, Long> wordCounts = Arrays.stream(allContent.split("\\s+"))
                .filter(word -> word.length() >= 2) // 한 글자 단어(은, 는, 이, 가 등)는 제외
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        // 가장 많이 나온 순서대로 상위 30개 추출
        return wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .map(entry -> new WordCloudDto(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }
}