package com.example.egobook_be.domain.ego_room.service;

import com.example.egobook_be.domain.diary.exception.DiaryErrorCode;
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
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EgoRoomService {

    private final DailyPraiseRepository dailyPraiseRepository;
    private final WeeklyCounselRepository weeklyCounselRepository;
    private final UserRepository userRepository;


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
        DailyPraise praise = dailyPraiseRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new CustomException(DiaryErrorCode.PRAISE_NOT_FOUND));
        List<RewardDto> rewards = new java.util.ArrayList<>();

        if (!praise.isRead()) {
            praise.markAsRead();

            Ability ability = praise.getDiary().getUser().getAbility();
          //  ability.addSelfEsteem(1); -> 대체 예정

//            rewards.add(new RewardDto(
//                    "SELF_ESTEEM",
//                    1,
//                    "칭찬서가 도착하여 자존감이 한 칸 상승했어요"
//            ));
        }

        return new DailyPraiseItemDto(
                praise.getDiary().getDate().toString(),
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


}