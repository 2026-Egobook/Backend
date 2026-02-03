package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EgoRoomService {

    private final DailyPraiseRepository dailyPraiseRepository;
    private final WeeklyCounselRepository weeklyCounselRepository;
    private final UserRepository userRepository;
    private final DailyPraiseAiService dailyPraiseAiService;
    private final WeeklyAnalysisAiService weeklyAnalysisAiService;
    private final DiaryRepository diaryRepository;

    @Transactional
    public void updateDailyPraiseSetting(User user, Boolean enabled) {
        if (enabled != null) {
            user.setDailyPraise(enabled);
        }
    }

    @Transactional
    public void updateWeeklyAnalysisSetting(User user, Boolean enabled) {
        if (enabled != null) {
            user.setWeeklyAnalysisEnabled(enabled);
        }
    }

    @Transactional(readOnly = true)
    public SliceResponse<DailyPraiseSimpleItemDto> getDailyPraiseList(Long userId, int page, int size) {
        if (page<1){
            throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        }
        if (size<1 || size>100){
            throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);
        }

        Pageable pageable = PageRequest.of(
                page-1,
                size,
                Sort.by(Sort.Direction.DESC,"praiseDate")
        );

        Slice<DailyPraise> praiseSlice = dailyPraiseRepository.findAllByUserId(userId, pageable);

        return SliceResponse.of(praiseSlice,praise->new DailyPraiseSimpleItemDto(
                praise.getId(),
                praise.getPraiseDate().toString(),
                praise.isRead()
        ));

    }

    @Transactional
    public DailyPraiseItemDto getDailyPraiseDetail(Long userId, LocalDate date) {
        DailyPraise praise = dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.PRAISE_NOT_FOUND));
        List<RewardDto> rewards = new java.util.ArrayList<>();

        if (!praise.isRead()) {
            praise.markAsRead();

          //  ability.addSelfEsteem(1); -> 대체 예정

//            rewards.add(new RewardDto(
//                    "SELF_ESTEEM",
//                    1,
//                    "칭찬서가 도착하여 자존감이 한 칸 상승했어요"
//            ));
        }

        return new DailyPraiseItemDto(
                praise.getPraiseDate().toString(),
                praise.getContent(),
                praise.getCreatedAt().toString(),
                praise.isRead(),
                rewards
        );
    }

    @Transactional(readOnly = true)
    public SliceResponse<WeeklyCounselSimpleItemDto> getWeeklyCounselList(Long userId, int page, int size) {
        if (page < 1) {
            throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        }
        if (size < 1 || size > 100) {
            throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "startDate")
        );

        Slice<WeeklyCounsel> counselSlice = weeklyCounselRepository.findAllByUserId(userId, pageable);

        return SliceResponse.of(counselSlice, counsel -> new WeeklyCounselSimpleItemDto(
                counsel.getId(),
                counsel.getStartDate().toString(),
                counsel.getEndDate().toString(),
                counsel.isRead()
        ));
    }

    @Transactional
    public WeeklyCounselDetailResDto getWeeklyCounselDetail(Long userId, LocalDate startDate) {
        WeeklyCounsel counsel = weeklyCounselRepository.findByUserIdAndStartDate(userId, startDate)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.COUNSEL_NOT_FOUND));
        counsel.markAsRead();
        return WeeklyCounselDetailResDto.from(counsel);
    }


    @Transactional
    public CounselToneResDto updateNextWeekTone(Long userId, CounselTone toneStyle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateCounselingTone(toneStyle);

        return new CounselToneResDto(toneStyle);
    }



    // 일간 칭찬서 생성
    @Transactional
    public void createDailyPraise(Long userId, LocalDate date) {

        // 이미 존재하면 넘어감
        Optional<DailyPraise> existingPraise = dailyPraiseRepository.findByUserIdAndPraiseDate(userId, date);
        if (existingPraise.isPresent()) {
            log.info("이미 생성된 칭찬이 있어 스킵합니다. (유저: {}, 날짜: {})", userId, date);
            existingPraise.get();
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다. ID: " + userId));
        List<Diary> diaries = diaryRepository.findByUserIdAndDate(userId, date);

        //다이어리가 없으면 넘어감
        if (diaries.isEmpty()) return;

        // 일기 내용들을 하나로 묶기
        String combinedContent = diaries.stream()
                .map(Diary::getContent)
                .collect(Collectors.joining("\n"));


        String praiseContent = dailyPraiseAiService.getPraise(combinedContent);

        DailyPraise dailyPraise = DailyPraise.builder()
                .user(user)
                .praiseDate(date)
                .content(praiseContent)
                .build();


        dailyPraiseRepository.save(dailyPraise);
    }

    // 주간 분석서 생성 로직
    @Transactional
    public void createWeeklyAnalysis(Long userId, LocalDate startDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (weeklyCounselRepository.existsByUserIdAndStartDate(userId, startDate)) {
            return;
        }

        String lastSummary = weeklyCounselRepository.findTopByUserOrderByEndDateDesc(user)
                .map(WeeklyCounsel::getSummary)
                .orElse("첫 주 분석입니다. 이전 데이터가 없습니다.");

        String userTone = (user.getCounselingTone() != null) ? user.getCounselingTone().name() : "SOFT";

        LocalDate endDate = startDate.plusDays(6);

        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate);
        if (diaries.isEmpty()) return;

        String formattedDiaries = diaries.stream()
                .collect(Collectors.groupingBy(
                        diary -> diary.getCreatedAt().toLocalDate(), // 날짜별로 그룹핑
                        TreeMap::new, // 날짜 순서대로 정렬
                        Collectors.mapping(Diary::getContent, Collectors.joining("\n- ")) // 내용은 리스트 형태로
                ))
                .entrySet().stream()
                .map(entry -> String.format("[%s]\n- %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n\n")); // 날짜별로 두 칸 띄우기


        WeeklyCounselResDto aiResponse = weeklyAnalysisAiService.getAnalysis(formattedDiaries,lastSummary, userTone);

        WeeklyCounsel counsel = WeeklyCounsel.builder()
                .user(user)
                .startDate(startDate)
                .endDate(endDate)
                .summary(aiResponse.summary())
                .praisePoints(aiResponse.praisePoints())
                .improvementPoints(aiResponse.improvementPoints())
                .managementAdvice(aiResponse.managementAdvice())
                .supportMessage(aiResponse.supportMessage())
                .build();

        weeklyCounselRepository.save(counsel);
//        log.info("[AI 저장 완료] 유저 {}의 주간 분석이 성공적으로 저장되었습니다.", userId);

    }


}