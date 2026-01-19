package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.domain.PlazaLetter;
import com.example.egobook_be.domain.letters.domain.PlazaLetterReply;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.dto.*;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterThreadRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.response.SliceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.egobook_be.domain.letters.domain.*;
import com.example.egobook_be.domain.letters.dto.*;



import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterService {

    private static final int REPLY_TEXT_LIMIT = 350;
    private static final int LETTER_TEXT_LIMIT = 360;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final PlazaLetterThreadRepository plazaLetterThreadRepository;

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

    @Transactional
    public CreateLetterResponse createLetter(Long userId, CreateLetterRequest request) {
        validateCreateLetterRequest(request);

        OffsetDateTime now = OffsetDateTime.now();

        // 하루 1회 제한 (KST 기준)
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        OffsetDateTime start = today.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        if (plazaLetterRepository.existsBySenderIdAndCreatedAtBetween(userId, start, end)) {
            throw new CustomException(LettersErrorCode.DAILY_LETTER_LIMIT);
        }

        if (!passesModeration(request.getText())) {
            throw new CustomException(LettersErrorCode.AI_MODERATION_FAILED);
        }

        // 1) 스레드 먼저 생성
        PlazaLetterThread thread = plazaLetterThreadRepository.save(PlazaLetterThread.createNow());

        // 2) 편지 생성 (RANDOM/FRIEND 매칭 로직은 다음 단계에서 붙이기)
        PlazaLetter letter = PlazaLetter.builder()
                .threadId(thread.getThreadId())
                .senderId(userId)
                .receiverId(null)
                .status(PlazaLetterStatus.SENT)
                .mode(request.getMode())
                .fromLabel("익명")
                .content(request.getText())
                .createdAt(now)
                .backgroundColor(request.getBackgroundColor())
                .arrivedAt(null)
                .replyDeadlineAt(null)
                .repliedAt(null)
                .gaveUpAt(null)
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

        if (!passesModeration(text)) {
            throw new CustomException(LettersErrorCode.AI_MODERATION_FAILED);
        }

        plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .threadId(letter.getThreadId())   // 중요
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

    private boolean passesModeration(String text) {
        //목록은 pm과 이야기해봐야함
        String lowered = text.toLowerCase();
        String[] banned = {"시발", "ㅅㅂ", "병신", "멍청", "죽어", "꺼져"};
        for (String b : banned) {
            if (lowered.contains(b)) return false;
        }
        return true;
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
