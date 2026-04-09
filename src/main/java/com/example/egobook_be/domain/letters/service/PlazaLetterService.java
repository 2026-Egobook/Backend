package com.example.egobook_be.domain.letters.service;


import com.example.egobook_be.domain.friend.repository.FriendRepository;
import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.home.repository.MissionRepository;
import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.dto.request.CreateLetterRequest;
import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import com.example.egobook_be.domain.letters.dto.response.*;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.enums.PlazaLetterColor;
import com.example.egobook_be.domain.letters.entity.BadWordBlockLog;
import com.example.egobook_be.domain.letters.enums.BlockType;
import com.example.egobook_be.domain.letters.repository.*;
import com.example.egobook_be.domain.notification.enums.NotificationType;
import com.example.egobook_be.domain.notification.service.NotificationService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlazaLetterService {

    private static final int REPLY_TEXT_LIMIT = 350;
    private static final int LETTER_TEXT_LIMIT = 360;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterThreadRepository plazaLetterThreadRepository;
    private final FriendRepository friendRepository;
    private final InkLogRepository inkLogRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final AbilityRepository abilityRepository;
    private final WordClientService wordClient;
    private final BadWordBlockLogRepository badWordBlockLogRepo;
    private final InkLogUtil inkLogUtil;

    private final PlazaLetterMapper plazaLetterMapper;

    private final NotificationService notificationService;

    private final AiRequestCountLogRepository aiRequestCountLogRepo;

    @Value("${spring.cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;


    @Transactional(readOnly = true)
    public InboxNextResponse getNextArrivedLetter(Long userId) {
        return plazaLetterRepository
                .findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(userId, PlazaLetterStatus.ARRIVED)
                .map(plazaLetterMapper::toResponse)  // Mapper를 이용해 변환
                .orElseGet(InboxNextResponse::empty);
    }

    private void enforceWordAiOrThrow(String text, Long userId, BlockType blockType) {
        // AI 요청 수 카운트 저장
        aiRequestCountLogRepo.save(AiRequestCountLog.builder()
                .type(blockType)
                .requestedAt(LocalDateTime.now())
                .build());
        try {
            WordDetectResponse res = wordClient.detect(text);
            if (wordClient.shouldBlock(res)) {
                badWordBlockLogRepo.save(BadWordBlockLog.builder()
                        .userId(userId)
                        .type(blockType)
                        .originalText(text)
                        .badWords(res.getBadWords() != null ? res.getBadWords() : List.of())
                        .score(res.getPercentage() / 100.0)
                        .blockedAt(LocalDateTime.now())
                        .build());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.USER_NOT_FOUND));
        Mission userMission = missionRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(UserErrorCode.MISSION_NOT_FOUND));

        validateCreateLetterRequest(request);

        LocalDateTime now = LocalDateTime.now();
        enforceDailyLimit(userId, now);


        if (request.getMode() == PlazaLetterMode.FRIEND) {
            User receiver = userRepository.findById(request.getToFriendId())
                    .orElseThrow(() -> new CustomException(LettersErrorCode.USER_NOT_FOUND));
            if (!friendRepository.existsByUserAndFriend(user, receiver)) {
                throw new CustomException(LettersErrorCode.NOT_FRIEND);
            }
        }

        PlazaLetterColor color = (request.getBackgroundColor() == null)
                ? PlazaLetterColor.WHITE : request.getBackgroundColor();
        int price = color.getPrice();
        if (price > 0) {
            user.useInk(price);
            inkLogRepository.save(InkLog.builder()
                    .user(user).amount(-price)
                    .reason(InkLogType.LETTER_COLOR_PURCHASE).build());
        }

        PlazaLetterThread thread = plazaLetterThreadRepository.save(PlazaLetterThread.createNow());

        String fromLabel = "익명";
        if (request.getMode() == PlazaLetterMode.FRIEND) {
            String nickname = user.getNickname();
            fromLabel = (nickname == null || nickname.isBlank()) ? "친구" : nickname;
        }

        Long receiverId = null;
        LocalDateTime arrivedAt = null;
        LocalDateTime replyDeadlineAt = null;

        if (request.getMode() == PlazaLetterMode.FRIEND) {
            receiverId = request.getToFriendId();
            arrivedAt = now;
            replyDeadlineAt = now.plusHours(24);
        } else {
            List<Long> candidates = userRepository.findHighReplyRateCandidates(userId, 50);
            if (!candidates.isEmpty()) {
                receiverId = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                arrivedAt = now;
                replyDeadlineAt = now.plusHours(24);
            }
        }

        // 처음엔 ANALYZING 상태로 저장
        PlazaLetter letter = PlazaLetter.builder()
                .threadId(thread.getThreadId())
                .senderId(userId)
                .receiverId(receiverId)
                .mode(request.getMode())
                .fromLabel(fromLabel)
                .content(request.getText())
                .backgroundColor(color)
                .status(PlazaLetterStatus.ANALYZING)
                .createdAt(now)
                .arrivedAt(arrivedAt)
                .replyDeadlineAt(replyDeadlineAt)
                .build();

        PlazaLetter saved = plazaLetterRepository.save(letter);


        List<InkLog> inkLogs = new ArrayList<>();
        inkLogUtil.addInkLogToList(inkLogs, user, 1, InkLogType.FIRST_LETTER_WRITE);
        if (userMission.updateDailyLetterMissionStatus(true)) {
            inkLogUtil.addInkLogToList(inkLogs, user, 1, InkLogType.DAILY_MISSION_REWARD);
            if (userMission.isWeeklyMissionCompleted()) {
                inkLogUtil.addInkLogToList(inkLogs, user, 2, InkLogType.WEEKLY_MISSION_REWARD);
            }
        }
        inkLogRepository.saveAll(inkLogs);

        //  AI 비동기 호출 시작
        Long savedLetterId = saved.getLetterId();
        Long finalReceiverId = receiverId;
        wordClient.detectAsync(request.getText())
                .subscribe(
                        result -> handleAiResult(savedLetterId, result, finalReceiverId, now, request.getMode(), user),
                        error  -> handleAiError(savedLetterId)
                );

        // AI 기다리지 않고 바로 응답
        String imageUrl = null;
        if (saved.getBackgroundColor() != PlazaLetterColor.WHITE) {
            String fileName = saved.getBackgroundColor().name().substring(0, 1)
                    + saved.getBackgroundColor().name().substring(1).toLowerCase();
            imageUrl = cloudfrontDomain + "/letter/" + fileName + ".png";
        }

        return CreateLetterResponse.builder()
                .letterId(saved.getLetterId())
                .threadId(saved.getThreadId())
                .status(saved.getStatus())
                .mode(saved.getMode())
                .fromLabel(saved.getFromLabel())
                .backgroundColor(saved.getBackgroundColor().name())
                .backgroundImageUrl(imageUrl)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // AI 결과 수신 처리
    @Transactional
    public void handleAiResult(Long letterId, WordDetectResponse result,
                               Long receiverId, LocalDateTime now,
                               PlazaLetterMode mode, User sender) {
        aiRequestCountLogRepo.save(AiRequestCountLog.builder()
                .type(BlockType.LETTER)
                .requestedAt(LocalDateTime.now())
                .build());

        PlazaLetter letter = plazaLetterRepository.findById(letterId).orElse(null);
        if (letter == null) return;

        // 취소됐으면 결과 무시
        if (letter.getStatus() == PlazaLetterStatus.CANCELLED) {
            return;
        }

        if (wordClient.shouldBlock(result)) {
            // 욕설 감지 → 차단 로그 저장 후 편지 삭제
            badWordBlockLogRepo.save(BadWordBlockLog.builder()
                    .userId(letter.getSenderId())
                    .type(BlockType.LETTER)
                    .originalText(result.getText())
                    .badWords(result.getBadWords() != null ? result.getBadWords() : List.of())
                    .score(result.getPercentage() / 100.0)
                    .blockedAt(LocalDateTime.now())
                    .build());
            plazaLetterRepository.delete(letter);
            return;
        }

        // 정상 → 원래 status로 변경
        PlazaLetterStatus finalStatus = (receiverId != null)
                ? PlazaLetterStatus.ARRIVED
                : PlazaLetterStatus.WAITING;
        letter.setStatus(finalStatus);
        plazaLetterRepository.save(letter);

        // 알림 발송
        try {
            if (mode == PlazaLetterMode.FRIEND) {
                notificationService.createNotification(
                        receiverId, NotificationType.LETTER_NEW_FRIEND,
                        letterId, sender.getNickname());
            } else if (receiverId != null) {
                notificationService.createNotification(
                        receiverId, NotificationType.LETTER_NEW, letterId);
            }
        } catch (Exception e) {
            log.error("편지 알림 생성 실패. LetterId: {}", letterId, e);
        }
    }

    // AI 서버 오류 처리
    @Transactional
    public void handleAiError(Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId).orElse(null);
        if (letter == null) return;
        if (letter.getStatus() == PlazaLetterStatus.CANCELLED) return;

        // AI 오류 시 욕설 감지 실패와 동일하게 처리
        plazaLetterRepository.delete(letter);
    }

    @Transactional
    public void cancelAnalysis(Long userId, Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        // 본인 편지인지 확인
        if (!userId.equals(letter.getSenderId())) {
            throw new CustomException(LettersErrorCode.FORBIDDEN);
        }

        // ANALYZING 상태일 때만 취소 가능
        if (letter.getStatus() == PlazaLetterStatus.ANALYZING) {
            letter.setStatus(PlazaLetterStatus.CANCELLED);
            plazaLetterRepository.save(letter);
        }
    }


    private void enforceDailyLimit(Long userId, LocalDateTime now) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime start = today.atStartOfDay(zoneId).toLocalDateTime();
        LocalDateTime end = today.plusDays(1).atStartOfDay(zoneId).toLocalDateTime();

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


    private PlazaLetterColor resolveBackgroundColor(String requested) {
        if (requested == null || requested.isBlank()) {
            return PlazaLetterColor.WHITE; // 기본 값: WHITE
        }
        try {
            return PlazaLetterColor.valueOf(requested.trim().toUpperCase()); // Enum으로 변환
        } catch (IllegalArgumentException e) {
            return PlazaLetterColor.WHITE; // 잘못된 값은 기본 WHITE로 처리
        }
    }



    private void validateCreateLetterRequest(CreateLetterRequest request) {
        if (request == null) {
            throw new CustomException(LettersErrorCode.INVALID_MODE);
        }

        if (request.getMode() == null) {
            throw new CustomException(LettersErrorCode.INVALID_MODE);
        }

        String text = request.getText();
        if (text == null || text.isBlank() || text.length() > LETTER_TEXT_LIMIT) {
            throw new CustomException(LettersErrorCode.LETTER_TEXT_LIMIT);
        }

        if (request.getMode() == PlazaLetterMode.FRIEND && request.getToFriendId() == null) {
            throw new CustomException(LettersErrorCode.FRIEND_ID_REQUIRED);
        }

        // backgroundColor는 null 허용.
        // null/WHITE면 이후 createLetter에서 WHITE로 보정 + price=0 처리함.
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
        LocalDateTime now = LocalDateTime.now();
        validateReplyable(letter, now);

        // 5. 해당 편지에 대해 답장이 이미 되어있는 상태인지 확인
        if (plazaLetterReplyRepository.existsByLetter(letter)) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }

        enforceWordAiOrThrow(text, userId, BlockType.REPLY);

        // 6. plazaLetterReplyRepository로 PlazaLetterReply를 저장하기 전, 해당 사용자가 답장한 편지가 오늘 처음 답장한 편지인지 확인한다.
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime startOfDay = today.atStartOfDay(zoneId).toLocalDateTime();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(zoneId).toLocalDateTime();
        boolean isFirstReplyToday = !plazaLetterReplyRepository.existsByReplierIdAndCreatedAtBetween(userId, startOfDay, endOfDay);

        PlazaLetterReply reply = plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .threadId(letter.getThreadId())
                .letter(letter)
                .replierId(userId)
                .text(text)
                .isAiGenerated(false)
                .createdAt(now)
                .build());

        letter.markReplied(now);

        // 답장 편지 수신자 알림 생성
        try {
            if (letter.getMode() == PlazaLetterMode.FRIEND) {
                notificationService.createNotification(
                        letter.getSenderId(),
                        NotificationType.LETTER_REPLY_FRIEND,
                        reply.getReplyId(),
                        user.getNickname()
                );
            } else {
                notificationService.createNotification(
                        letter.getSenderId(),
                        NotificationType.LETTER_REPLY,
                        reply.getReplyId()
                );
            }
        } catch (Exception e) {
            log.error("답장 편지 도착 알림 생성 실패. SenderId: {}, ReplyId: {}",
                    letter.getSenderId(), reply.getReplyId(), e);
        }

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
            inkLogUtil.addInkLogToList(inkLogs, user, 1, InkLogType.FIRST_LETTER_REPLY); // 잉크 +1
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
                inkLogUtil.addInkLogToList(inkLogs, user, earnedInk, InkLogType.LEVEL_UP);
                user.levelUp();
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

        LocalDateTime now = LocalDateTime.now();
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

        LocalDateTime now = LocalDateTime.now();
        validateGiveUpable(letter, now);

        // 유저 가져오기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 포기 처리
        letter.markGaveUp(now);

        // 4시간 수신 제한
        user.blockLetterReceiveUntil(now.plusHours(4));

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

    private void validateReplyable(PlazaLetter letter, LocalDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }
    }

    private void validateDeferable(PlazaLetter letter, LocalDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }

    }

    private void validateGiveUpable(PlazaLetter letter, LocalDateTime now) {
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }
        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || isDeadlinePassed(letter, now)) {
            throw new CustomException(LettersErrorCode.ALREADY_GAVE_UP);
        }
    }

    private boolean isDeadlinePassed(PlazaLetter letter, LocalDateTime now) {
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
                plazaLetterReplyRepository.findMyRepliesWithLetter(userId, pageable),
                this::toReplyItemDto
        );
    }

    private ReplyItemDto toReplyItemDto(PlazaLetterReply reply) {
        PlazaLetter letter = reply.getLetter();

        return ReplyItemDto.builder()
                .replyId(reply.getReplyId())
                .letterId(letter.getLetterId())
                .threadId(reply.getThreadId())
                .mode(letter.getMode())
                .backgroundColor(letter.getBackgroundColor().name())
                .replyText(reply.getText())
                .createdAt(reply.getCreatedAt())
                .build();
    }

}