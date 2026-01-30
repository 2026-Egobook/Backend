package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.entity.Diary;
import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
import com.example.egobook_be.domain.diary.repository.DiaryRepository;
import com.example.egobook_be.domain.ego_room.dto.CounselTonePatchReqDto;
import com.example.egobook_be.domain.ego_room.dto.*;
import com.example.egobook_be.domain.ego_room.entity.DailyPraise;
import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import com.example.egobook_be.domain.ego_room.enums.CounselTone;
import com.example.egobook_be.domain.ego_room.exception.EgoRoomErrorCode;
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
import java.util.Map;
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
                        praise.getPraiseDate().toString(), // 이제 diary가 아니라 praise 자체에서 날짜를 가져와!
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
                praise.getDiary().getDate().toString(), // writtenAt 대신 date 사용
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

        List<WeeklyCounselSimpleItemDto> values = counselSlice.getContent().stream()
                .map(counsel -> new WeeklyCounselSimpleItemDto(
                        counsel.getId(),
                        counsel.getStartDate().toString(),
                        counsel.getEndDate().toString(),
                        counsel.isRead()
                ))
                .collect(Collectors.toList());

        Long nextCursor = counselSlice.hasNext() ?
                counselSlice.getContent().get(counselSlice.getContent().size() - 1).getId() : null;

        return new WeeklyCounselListResDto(values, counselSlice.hasNext(), nextCursor);
    }

    @Transactional(readOnly = true)
    public WeeklyCounselDetailResDto getWeeklyCounselDetail(Long userId, LocalDate startDate) {
        WeeklyCounsel counsel = weeklyCounselRepository.findByUserIdAndStartDate(userId, startDate)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.COUNSEL_NOT_FOUND));

        return WeeklyCounselDetailResDto.from(counsel);
    }


    @Transactional
    public CounselToneResDto updateNextWeekTone(Long userId, CounselTone toneStyle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateCounselingTone(toneStyle);

        return new CounselToneResDto(toneStyle);
    }


}