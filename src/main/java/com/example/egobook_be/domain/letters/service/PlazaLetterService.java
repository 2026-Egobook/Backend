package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.diary.dto.DiaryCreateResDto;
import com.example.egobook_be.domain.diary.enums.RewardType;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.dto.request.CreateLetterRequest;
import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import com.example.egobook_be.domain.letters.dto.response.*;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.enums.UserErrorCode;
import com.example.egobook_be.domain.user.repository.AbilityRepository;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.domain.letters.mapper.PlazaLetterMapper;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import com.example.egobook_be.global.util.InkLogUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PlazaLetterService {

    private static final int REPLY_TEXT_LIMIT = 350;
    private static final int LETTER_TEXT_LIMIT = 360;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterThreadRepository plazaLetterThreadRepository;
    private final InkLogRepository inkLogRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final AbilityRepository abilityRepository;
    private final WordClientService wordClient;
    private final InkLogUtil inkLogUtil;


    private final PlazaLetterMapper plazaLetterMapper;

    @Transactional(readOnly = true)
    public InboxNextResponse getNextArrivedLetter(Long userId) {
        return plazaLetterRepository
                .findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(userId, PlazaLetterStatus.ARRIVED)
                .map(plazaLetterMapper::toResponse)  // Mapper를 이용해 변환
                .orElseGet(InboxNextResponse::empty);
    }



    private void enforceWordAiOrThrow(String text) {
        try {
            WordDetectResponse res = wordClient.detect(text);
            if (wordClient.shouldBlock(res)) {
                throw new CustomException(LettersErrorCode.AI_MODERATION_FAILED);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            // AI 서버 장애/타임아웃일 때 예외처리
            throw new CustomException(LettersErrorCode.AI_MODERATION_FAILED);
        }
    }

    @Transactional
    public CreateLetterResponse createLetter(Long userId, CreateLetterRequest request) {
        // 1. User, Mission 객체 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(LettersErrorCode.USER_NOT_FOUND));
        Mission userMission = missionRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.MISSION_NOT_FOUND));

        // 2. 들어온 CreateLetterRequest 검증
        validateCreateLetterRequest(request);

        // 3. 해당 사용자가 편지를 작성할 수 있는 상태인지(이미 오늘 편지를 작성하였는지) 여부 검증
        OffsetDateTime now = OffsetDateTime.now();
        enforceDailyLimit(userId, now);

        enforceWordAiOrThrow(request.getText());

        PlazaLetterThread thread = plazaLetterThreadRepository.save(PlazaLetterThread.createNow());
        Long receiverId = resolveReceiverId(userId, request);
        String bg = resolveBackgroundColor(request.getBackgroundColor());

        String fromLabel = "익명";
        if (request.getMode() == PlazaLetterMode.FRIEND) {
            String nickname = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(LettersErrorCode.USER_NOT_FOUND))
                    .getNickname();
            fromLabel = (nickname == null || nickname.isBlank()) ? "친구" : nickname;
        }

        PlazaLetter letter = PlazaLetter.builder()
                .threadId(thread.getThreadId())
                .senderId(userId)
                .receiverId(receiverId)
                .mode(request.getMode())
                .fromLabel(fromLabel)
                .content(request.getText())
                .backgroundColor(bg)
                .status(PlazaLetterStatus.ARRIVED)
                .createdAt(now)
                .arrivedAt(now)
                .replyDeadlineAt(now.plusHours(24))
                .build();

        // =================================================
        // [ 보상 제공 로직 ] - 이미 enforceDailyLimit() 에서 오늘 편지를 썼는지 안썼는지 여부를 검사하였음 (하루에 1개의 편지 작성이 최대값임)
        // =================================================
        List<InkLog> inkLogs = new ArrayList<>();
        // 1. 잉크 제공 & 일일 미션 상태 업데이트
        inkLogUtil.addInkLog(inkLogs, user, 1, InkLogType.FIRST_LETTER_WRITE);
        /*
         * 1-1. 만약 이번이 처음 일일 미션을 수행한 경우일 때 (updateDailyLetterMissionStatus 함수가 true를 반환)
         * - 잉크 +1
         * - 잉크 로그 추가
         */
        if(userMission.updateDailyLetterMissionStatus(true)){
            inkLogUtil.addInkLog(inkLogs, user, 1, InkLogType.DAILY_MISSION_REWARD);
            /*
             * 1-2. 만약 오늘이 일일 미션을 7일째 완료한 날인 경우
             * - 잉크 +2
             * - 잉크 로그 추가
             */
            if(userMission.isWeeklyMissionCompleted()){
                inkLogUtil.addInkLog(inkLogs, user, 2, InkLogType.WEEKLY_MISSION_REWARD);
            }
        }
        inkLogRepository.saveAll(inkLogs);

        PlazaLetter saved = plazaLetterRepository.save(letter);

        return CreateLetterResponse.builder()
                .letterId(saved.getLetterId())
                .threadId(saved.getThreadId())
                .status(saved.getStatus())
                .mode(saved.getMode())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void enforceDailyLimit(Long userId, OffsetDateTime now) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        OffsetDateTime start = today.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        if (plazaLetterRepository.existsBySenderIdAndCreatedAtBetween(userId, start, end)) {
            throw new CustomException(LettersErrorCode.DAILY_LETTER_LIMIT);
        }
    }

    private Long resolveReceiverId(Long userId, CreateLetterRequest request) {
        if (request.getMode() == PlazaLetterMode.FRIEND) {
            // friend 관계 검증(친구 맞는지) 추가해야함
            return request.getToFriendId();
        }

        // RANDOM모드
        List<Long> candidates = userRepository.findHighReplyRateCandidates(userId, 50);

        if (candidates.isEmpty()) {
            throw new CustomException(LettersErrorCode.NO_RECEIVER_AVAILABLE);
        }

        int pick = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(pick);
    }


    private String resolveBackgroundColor(String requested) {
        if (requested == null || requested.isBlank()) {
            return "WHITE";
        }
        return requested.trim().toUpperCase();
    }


    private void validateCreateLetterRequest(CreateLetterRequest request) {
        if (request == null || request.getMode() == null) {
            throw new CustomException(LettersErrorCode.INVALID_MODE);
        }
        String text = request.getText();
        if (text == null || text.isBlank() || text.length() > LETTER_TEXT_LIMIT) {
            throw new CustomException(LettersErrorCode.LETTER_TEXT_LIMIT);
        }
        if (request.getMode() == PlazaLetterMode.FRIEND && request.getToFriendId() == null) {
            throw new CustomException(LettersErrorCode.FRIEND_ID_REQUIRED);
        }
    }

    @Transactional
    public DeleteThreadResponse deleteThread(Long userId, Long threadId) {
        // 스레드 존재 체크
        if (!plazaLetterThreadRepository.existsById(threadId)) {
            throw new CustomException(LettersErrorCode.THREAD_NOT_FOUND);
        }

        // 스레드에 속한 편지 조회 (스레드당 편지 1개라는 전제)
        PlazaLetter letter = plazaLetterRepository.findByThreadId(threadId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.THREAD_NOT_FOUND));

        // 권한: senderId 또는 receiverId가 나일 때만 삭제 가능
        boolean mine = userId != null
                && (userId.equals(letter.getSenderId()) || (letter.getReceiverId() != null && userId.equals(letter.getReceiverId())));

        if (!mine) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }

        // replies -> letters -> thread 순서로 삭제
        plazaLetterReplyRepository.deleteByThreadId(threadId);
        plazaLetterRepository.deleteByThreadId(threadId);
        plazaLetterThreadRepository.deleteById(threadId);

        return DeleteThreadResponse.builder()
                .threadId(threadId)
                .deleted(true)
                .build();
    }

    @Transactional
    public ReplyResponse replyToLetter(Long userId, Long letterId, String text) {
        // 1. User, Ability 가져오기
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Ability userAbility = abilityRepository.findByUser(user).orElseThrow(() -> new CustomException(UserErrorCode.ABILITY_NOT_FOUND));

        // 2. 답장할 텍스트 검증
        validateReplyText(text);

        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        // 3. 자기 자신에게 답장을 보내지 않도록 검증
        validateOwnership(letter, userId);

        // 4. 답장 가능한 상태인지 검증
        OffsetDateTime now = OffsetDateTime.now();
        validateReplyable(letter, now);

        // 5. 해당 편지에 대해 답장이 이미 되어있는 상태인지 확인
        if (plazaLetterReplyRepository.existsByLetterId(letter.getLetterId())) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }

        enforceWordAiOrThrow(text);

        // 6. plazaLetterReplyRepository로 PlazaLetterReply를 저장하기 전, 해당 사용자가 답장한 편지가 오늘 처음 답장한 편지인지 확인한다.
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        OffsetDateTime startOfDay = today.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        boolean isFirstReplyToday = !plazaLetterReplyRepository.existsByReplierIdAndCreatedAtBetween(userId, startOfDay, endOfDay);

        plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .threadId(letter.getThreadId())
                .letterId(letter.getLetterId())
                .replierId(userId)
                .text(text)
                .isAiGenerated(false)
                .createdAt(now)
                .build());

        letter.markReplied(now);

        // =============================================
        // [ 보상 로직 ]
        // =============================================
        /*
         * 1. 해당 사용자가 오늘 처음 편지 답장을 한 경우
         * - 잉크 +1
         * - 잉크 로그 작성
         * - 공감성 +1
         * + 레벨업 시 잉크 +1
         */
        List<ReplyResponse.RewardDto> rewards = new ArrayList<>();
        List<InkLog> inkLogs = new ArrayList<>();
        if(isFirstReplyToday){
            inkLogUtil.addInkLog(inkLogs, user, 1, InkLogType.FIRST_LETTER_REPLY); // 잉크 +1
            rewards.add(ReplyResponse.RewardDto.builder()
                    .kind(ReplyResponse.RewardKind.INK)
                    .amount(1)
                    .toastMessage("첫 답장 보상으로 잉크를 획득했어요!")
                    .build());

            int earnedInk = userAbility.addEmpathy(1); // 공감성 +1
            rewards.add(ReplyResponse.RewardDto.builder()
                    .kind(ReplyResponse.RewardKind.EMPATHY) // [수정] SINCERITY -> EMPATHY
                    .amount(1)
                    .toastMessage("공감성 점수가 1 상승했어요.")
                    .build());
            // 1-1. 레벨업했는지 여부 확인
            if(earnedInk == 1){
                inkLogUtil.addInkLog(inkLogs, user, earnedInk, InkLogType.LEVEL_UP);
                rewards.add(ReplyResponse.RewardDto.builder()
                        .kind(ReplyResponse.RewardKind.EMPATHY) // [수정] SINCERITY -> EMPATHY
                        .amount(1)
                        .toastMessage("공감성 레벨이 1 상승했어요.")
                        .build());
            }
        }
        inkLogRepository.saveAll(inkLogs);

        return ReplyResponse.builder()
                .letterId(letter.getLetterId())
                .status(letter.getStatus())
                .repliedAt(now)
                .rewards(rewards)
                .build();
    }

    @Transactional
    public DeferResponse deferLetter(Long userId, Long letterId) {
        PlazaLetter letter = getLetterOrThrow(letterId);
        validateOwnership(letter, userId);

        OffsetDateTime now = OffsetDateTime.now();
        validateDeferable(letter, now);

        if (letter.getStatus() == PlazaLetterStatus.ARRIVED) {
            letter.markDeferred();
        }

        return DeferResponse.builder()
                .letterId(letter.getLetterId())
                .status(letter.getStatus())
                .build();
    }

    @Transactional
    public GiveUpResponse giveUpLetter(Long userId, Long letterId) {
        PlazaLetter letter = getLetterOrThrow(letterId);
        validateOwnership(letter, userId);

        OffsetDateTime now = OffsetDateTime.now();
        validateGiveUpable(letter, now);

        letter.markGaveUp(now);

        return GiveUpResponse.builder()
                .letterId(letter.getLetterId())
                .status(letter.getStatus())
                .gaveUpAt(letter.getGaveUpAt())
                .build();
    }

    // ======= 공통 검증/조회 메서드 =======

    private PlazaLetter getLetterOrThrow(Long letterId) {
        return plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));
    }

    private void validateOwnership(PlazaLetter letter, Long userId) {
        if (userId == null || !userId.equals(letter.getReceiverId())) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }
    }

    private void validateReplyText(String text) {
        if (text == null || text.isBlank()) {
            throw new CustomException(LettersErrorCode.REPLY_TEXT_LIMIT);
        }
        if (text.length() > REPLY_TEXT_LIMIT) {
            throw new CustomException(LettersErrorCode.REPLY_TEXT_LIMIT);
        }
    }

    private void validateReplyable(PlazaLetter letter, OffsetDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }
    }

    private void validateDeferable(PlazaLetter letter, OffsetDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }

    }

    private void validateGiveUpable(PlazaLetter letter, OffsetDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }
    }

    private boolean isDeadlinePassed(PlazaLetter letter, OffsetDateTime now) {
        return letter.getReplyDeadlineAt() != null && now.isAfter(letter.getReplyDeadlineAt());
    }


    @Transactional(readOnly = true)
    public SliceResponse<ReplyItemDto> getMyReplies(Long userId, int page, int size) {

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize
        );

        return SliceResponse.of(
                plazaLetterReplyRepository.findByReplierIdOrderByReplyIdDesc(userId, pageable),
                this::toReplyItemDto
        );
    }

    private ReplyItemDto toReplyItemDto(PlazaLetterReply reply) {
        return ReplyItemDto.builder()
                .replyId(reply.getReplyId())
                .letterId(reply.getLetterId())
                .replyText(reply.getText())
                .createdAt(reply.getCreatedAt())
                .build();
    }



}
