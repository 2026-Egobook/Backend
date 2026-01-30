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
    // private final SubscriptionRepository subscriptionRepository; =

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

        //광고보는 로직 추가 필요

        LocalDate now = LocalDate.now();
        LocalDateTime oneYearAgo = now.minusYears(1).atStartOfDay();

        // 1. 1년치 일기 데이터 조회 및 필터링
        List<Diary> yearlyDiaries = diaryRepository.findAllByUserIdAndWrittenAtAfter(userId, oneYearAgo)
                .stream()
                .filter(d -> d.getEmotionLevel() > 0)
                .toList();


        if (yearlyDiaries.isEmpty()) {
            return EgoStatsResDto.empty(year, month);
        }

        // 2. 무드별 요일/시간 통계 계산 (bars)
        MoodStatsDto bars = calculateMoodStats(yearlyDiaries);

        // 3. 요일별 감정 스택 통계 계산 (stacked)
        StackedStatsDto stacked = calculateStackedStats(yearlyDiaries);

        // 4. 6개월 평균 점수 계산 (sixMonthAvgs)
        List<MonthlyAvgDto> sixMonthAvgs = calculateSixMonthAvgs(userId, year, month);

        //워드클라우드
        List<WordCloudDto> wordCloud = calculateWordCloud(yearlyDiaries);

        return EgoStatsResDto.builder()
                .year(year)
                .month(month)
                .bars(bars)
                .stacked(stacked)
                .sixMonthAvgs(sixMonthAvgs)
                .wordCloud(wordCloud)
                .generatedAt(LocalDateTime.now())
                .build();
    }


    //내부 계산 로직

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

    private MoodStatsDto calculateMoodStats(List<Diary> diaries) {
        // 1. 굿 무드와 배드 무드 그룹핑
        List<Diary> goodDiaries = diaries.stream()
                .filter(d -> d.getEmotionLevel() >= 4)
                .toList();
        List<Diary> badDiaries = diaries.stream()
                .filter(d -> d.getEmotionLevel() >= 1 && d.getEmotionLevel() <= 2)
                .toList();

        return new MoodStatsDto(
                createMoodDetail(goodDiaries),
                createMoodDetail(badDiaries)
        );
    }

    private MoodDetailDto createMoodDetail(List<Diary> diaries) {
        // 요일별 카운트 (MON, TUE...)
        List<DayCountDto> byDayOfWeek = Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    long count = diaries.stream()
                            .filter(d -> d.getWrittenAt().getDayOfWeek() == day)
                            .count();
                    return new DayCountDto(day.name().substring(0, 3), (int) count);
                }).toList();

        // 시간별 카운트 (0-23시)
        List<HourCountDto> byHour = IntStream.range(0, 24)
                .mapToObj(hour -> {
                    long count = diaries.stream()
                            .filter(d -> d.getWrittenAt().getHour() == hour)
                            .count();
                    return new HourCountDto(hour, (int) count);
                }).toList();

        return new MoodDetailDto(byDayOfWeek, byHour);
    }

    private StackedStatsDto calculateStackedStats(List<Diary> diaries) {
        // 1. 요일별-레벨별 맵 생성
        Map<DayOfWeek, Map<Integer, Long>> weeklyMap = diaries.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getWrittenAt().getDayOfWeek(),
                        Collectors.groupingBy(Diary::getEmotionLevel, Collectors.counting())
                ));

        // 2. 전체 요일/레벨 통틀어 가장 높은 count 찾기 (100% 기준점)
        long maxCount = weeklyMap.values().stream()
                .flatMap(m -> m.values().stream())
                .max(Long::compare).orElse(0L);

        // 3. 요일별 데이터 생성
        List<WeekdayStackDto> byWeekday = Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    Map<Integer, Long> levelsMap = weeklyMap.getOrDefault(day, Collections.emptyMap());
                    List<LevelDto> levels = IntStream.rangeClosed(1, 5)
                            .mapToObj(lvl -> {
                                long count = levelsMap.getOrDefault(lvl, 0L);
                                int percent = maxCount == 0 ? 0 : (int) ((count * 100) / maxCount);
                                return new LevelDto(lvl, (int) count, percent);
                            }).toList();
                    return new WeekdayStackDto(day.name().substring(0, 3), levels);
                }).toList();

        return new StackedStatsDto((int) maxCount, byWeekday);
    }

    private List<WordCloudDto> calculateWordCloud(List<Diary> diaries) {
        // 1. 모든 일기의 본문을 하나로 합치고 특수문자 제거
        // 한글, 영어, 숫자만 남기고 나머지는 공백으로 치환해
        String allContent = diaries.stream()
                .map(Diary::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");

        // 2. 공백 기준으로 단어 분리 후 빈도수 계산
        Map<String, Long> wordCounts = Arrays.stream(allContent.split("\\s+"))
                .filter(word -> word.length() >= 2) // 한 글자 단어(은, 는, 이, 가 등)는 제외
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        // 3. 가장 많이 나온 순서대로 상위 30개 추출
        return wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .map(entry -> new WordCloudDto(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }
}