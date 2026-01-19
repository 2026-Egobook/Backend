package com.example.egobook_be.domain.letters.service;

import com.example.egobook_be.domain.letters.domain.PlazaLetter;
import com.example.egobook_be.domain.letters.domain.PlazaLetterReply;
import com.example.egobook_be.domain.letters.domain.PlazaLetterStatus;
import com.example.egobook_be.domain.letters.dto.DeferResponse;
import com.example.egobook_be.domain.letters.dto.GiveUpResponse;
import com.example.egobook_be.domain.letters.dto.InboxNextResponse;
import com.example.egobook_be.domain.letters.dto.ReplyResponse;
import com.example.egobook_be.domain.letters.enums.LettersErrorCode;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterService {

    private static final int REPLY_TEXT_LIMIT = 350;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;

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
    public ReplyResponse replyToLetter(Long userId, Long letterId, String text) {
        validateReplyText(text);

        PlazaLetter letter = getLetterOrThrow(letterId);
        validateOwnership(letter, userId);

        OffsetDateTime now = OffsetDateTime.now();
        validateReplyable(letter, now);

        // (선택) DB 기준으로도 멱등/레이스 방어 (Repository에 existsByLetterId가 있어야 함)
        if (plazaLetterReplyRepository.existsByLetterId(letter.getLetterId())) {
            throw new CustomException(LettersErrorCode.ALREADY_REPLIED);
        }

        if (!passesModeration(text)) {
            throw new CustomException(LettersErrorCode.AI_MODERATION_FAILED);
        }

        plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .letterId(letter.getLetterId())
                .replierId(userId)
                .text(text)
                .createdAt(now)
                .build());

        letter.markReplied(now);

        List<ReplyResponse.RewardDto> rewards = List.of(
                ReplyResponse.RewardDto.builder()
                        .kind(ReplyResponse.RewardKind.INK)
                        .amount(1)
                        .toastMessage(null)
                        .build(),
                ReplyResponse.RewardDto.builder()
                        .kind(ReplyResponse.RewardKind.SINCERITY)
                        .amount(1)
                        .toastMessage(null)
                        .build()
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
        // DEFERRED는 멱등 허용 (그대로 OK)
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
        // TODO: 실제 AI moderation 연결 전까지 스텁
        String lowered = text.toLowerCase();
        String[] banned = {"시발", "ㅅㅂ", "병신", "멍청", "죽어", "꺼져"};
        for (String b : banned) {
            if (lowered.contains(b)) return false;
        }
        return true;
    }
}
