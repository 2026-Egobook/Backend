package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.ego_room.dto.CounselTonePatchReqDto;
import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.ego_room.repository.DailyPraiseRepository;
import com.example.egobook_be.domain.ego_room.repository.WeeklyCounselRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.exception.UserErrorCode;
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
        log.info("현재 로그인한 유저 아이디: {}", userId);
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
}