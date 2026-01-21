package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.domain.PlazaLetter;
import com.example.egobook_be.domain.letters.domain.PlazaLetterReply;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.dto.request.CreateLetterRequest;
import com.example.egobook_be.domain.letters.dto.response.WordDetectResponse;
import com.example.egobook_be.domain.letters.dto.response.*;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.egobook_be.domain.letters.domain.*;


import java.time.*;
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
    private final UserRepository userRepository;
    private final WordClientService wordClient;


    @Transactional(readOnly = true)
    public InboxNextResponse getNextArrivedLetter(Long userId) {
        return plazaLetterRepository
                .findFirstByReceiverIdAndStatusOrderByArrivedAtDesc(userId, PlazaLetterStatus.ARRIVED)
                .map(this::toResponse)
                .orElseGet(InboxNextResponse::empty);
    }

    private InboxNextResponse toResponse(PlazaLetter letter) {
        return InboxNextResponse.builder()
                .letter(InboxNextResponse.LetterDto.builder()
                        .letterId(letter.getLetterId())
                        .status(letter.getStatus())
                        .mode(letter.getMode())
                        .fromLabel(letter.getFromLabel())
                        .content(letter.getContent())
                        .arrivedAt(letter.getArrivedAt())
                        .replyDeadlineAt(letter.getReplyDeadlineAt())
                        .build())
                .build();
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
        validateCreateLetterRequest(request);

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
        validateReplyText(text);

        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new CustomException(LettersErrorCode.LETTER_NOT_FOUND));

        validateOwnership(letter, userId);

        OffsetDateTime now = OffsetDateTime.now();
        validateReplyable(letter, now);

        if (plazaLetterReplyRepository.existsByLetterId(letter.getLetterId())) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }

        enforceWordAiOrThrow(text);

        plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .threadId(letter.getThreadId())
                .letterId(letter.getLetterId())
                .replierId(userId)
                .text(text)
                .isAiGenerated(false)
                .createdAt(now)
                .build());

        letter.markReplied(now);

        List<ReplyResponse.RewardDto> rewards = List.of(
                ReplyResponse.RewardDto.builder().kind(ReplyResponse.RewardKind.INK).amount(1).toastMessage(null).build(),
                ReplyResponse.RewardDto.builder().kind(ReplyResponse.RewardKind.SINCERITY).amount(1).toastMessage(null).build()
        );

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

        int safePage = Math.max(page, 0); // 0부터 시작
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

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
