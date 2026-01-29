package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.ego_room.dto.CounselTonePatchReqDto;
import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.ego_room.exception.SubscriptionLockedException;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
import com.example.egobook_be.domain.user.repository.SubscriptionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EgoRoomService {

    private final DailyPraiseRepository dailyPraiseRepository;
    private final WeeklyCounselRepository weeklyCounselRepository;
    private final UserRepository userRepository;

    public DailyPraiseListResDto getDailyPraiseList(Long userId, Long cursor, int size) {
        Slice<DailyPraise> praiseSlice = dailyPraiseRepository.findPraiseList(
                userId,
                cursor,
                PageRequest.of(0, size)
        );

        List<DailyPraiseSimpleItemDto> values = praiseSlice.getContent().stream()
                .map(praise -> new DailyPraiseSimpleItemDto(
                        praise.getId(),
                        praise.getDiary().getWrittenAt().toLocalDate().toString(),
                        praise.isRead()
                ))
                .collect(Collectors.toList());

        Long nextCursor = praiseSlice.hasNext() ?
                praiseSlice.getContent().get(praiseSlice.getContent().size() - 1).getId() : null;

        return new DailyPraiseListResDto(values, praiseSlice.hasNext(), nextCursor);
    }

    @Transactional
    public DailyPraiseItemDto getDailyPraiseDetail(Long userId, LocalDate date) {
        DailyPraise praise = dailyPraiseRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.PRAISE_NOT_FOUND));
        List<RewardDto> rewards = new java.util.ArrayList<>();

        if (!praise.isRead()) {
            praise.markAsRead();

            Ability ability = praise.getDiary().getUser().getAbility();
            ability.addSelfEsteem(1);

            rewards.add(new RewardDto(
                    "SELF_ESTEEM",
                    1,
                    "칭찬서가 도착하여 자존감이 한 칸 상승했어요"
            ));
        }

        return new DailyPraiseItemDto(
                praise.getDiary().getWrittenAt().toLocalDate().toString(),
                praise.getContent(),
                praise.getCreatedAt().toString(),
                praise.isRead(),
                rewards
        );
    }

    public WeeklyCounselListResDto getWeeklyCounselList(Long userId, Long cursor, int size) {

        Slice<WeeklyCounsel> counselSlice = weeklyCounselRepository.findWeeklyCounselList(
                userId,
                cursor,
                PageRequest.of(0, size)
        );
       // log.info("현재 로그인한 유저 아이디: {}", userId);
        log.info("DB에서 가져온 상담서 개수: {}", counselSlice.getContent().size()); // 이거 추가!

        List<WeeklyCounselItemDto> values = counselSlice.getContent().stream()
                .map(counsel -> new WeeklyCounselItemDto(
                        counsel.getId(),
                        counsel.getStartDate().toString().replace("-", "."),
                        counsel.isRead()
                ))
                .collect(Collectors.toList());

        Long nextCursor = counselSlice.hasNext() ?
                counselSlice.getContent().get(counselSlice.getContent().size() - 1).getId() : null;

        return new WeeklyCounselListResDto(values, counselSlice.hasNext(), nextCursor);
    }

    @Transactional
    public WeeklyCounselDetailResDto getWeeklyCounselDetail(Long userId, LocalDate startDate) {
        WeeklyCounsel counsel = weeklyCounselRepository.findByUserIdAndStartDate(userId, startDate)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.COUNSEL_NOT_FOUND));

        if (!counsel.isRead()) {
            counsel.unlock();
        }

        return new WeeklyCounselDetailResDto(
                counsel.getStartDate().toString().replace("-", "."),
                counsel.getEndDate().toString().replace("-", "."),
                counsel.getSummary(),
                counsel.getPraisePoints(),
                counsel.getImprovementPoints(),
                counsel.getManagementAdvice(),
                counsel.getSupportMessage(),
                counsel.isRead()
        );
    }

    @Transactional
    public CounselToneResDto updateNextWeekTone(Long userId, CounselTone toneStyle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateCounselingTone(toneStyle);

        return new CounselToneResDto(toneStyle);
    }

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 월간 통계 데이터 조회 (구독자 전용)
     */
    @Transactional(readOnly = true)
    public EgoStatsResDto getMonthlyStats(Long userId, int year, int month) {
        log.info("유저 {}번의 {}년 {}월 통계 조회 시도", userId, year, month);

        // 활성화된 구독 정보가 있는지 확인
        subscriptionRepository.findActiveSubscription(userId, LocalDate.now())
                .orElseThrow(SubscriptionLockedException::new);

        // 해당 월의 다이어리 데이터 가져오기
        // (작성 시간 기준으로 한 달 치 데이터를 긁어와야 해)
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

        // diaryRepository.findAllByUserIdAndWrittenAtBetween(userId, startOfMonth, endOfMonth) 같은 메서드가 필요해!

        log.info("구독 확인 완료! 이제 통계 계산 시작할게 다경!");

        // 3. TODO: 다이어리 데이터를 바탕으로 상단 바 그래프, 스택 그래프, 워드클라우드 데이터 계산 로직

        return null; // 우선 구조만 잡았어!
    }

}