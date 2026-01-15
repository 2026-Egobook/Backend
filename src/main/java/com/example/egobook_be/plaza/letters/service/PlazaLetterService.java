package com.example.egobook_be.plaza.letters.service;

import com.example.egobook_be.plaza.letters.domain.*;
import com.example.egobook_be.plaza.letters.dto.InboxNextResponse;
import com.example.egobook_be.plaza.letters.dto.ReplyResponse;
import com.example.egobook_be.plaza.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.plaza.letters.repository.PlazaLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PlazaLetterService {

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
        // 1) 글자수 방어(Validation이 있어도 한 번 더)
        if (text == null || text.isBlank()) {
            throw new PlazaLetterException("PLAZA400_REPLY_TEXT_LIMIT", "답장 내용을 입력해주세요");
        }
        if (text.length() > 350) {
            throw new PlazaLetterException("PLAZA400_REPLY_TEXT_LIMIT", "답장은 350자 이하여야 해요");
        }

        // 2) 편지 조회
        PlazaLetter letter = plazaLetterRepository.findById(letterId)
                .orElseThrow(() -> new PlazaLetterException("PLAZA404_LETTER_NOT_FOUND", "편지를 찾을 수 없어요"));

        // 3) 내 편지인지(내가 답장해야 할 편지인지) 체크
        if (!letter.getReceiverId().equals(userId)) {
            throw new PlazaLetterException("PLAZA403_FORBIDDEN", "접근 권한이 없어요");
        }

        // 4) 이미 포기 상태(또는 마감 지남) 체크
        OffsetDateTime now = OffsetDateTime.now();
        boolean deadlinePassed = now.isAfter(letter.getReplyDeadlineAt());

        if (letter.getStatus() == PlazaLetterStatus.GAVE_UP || deadlinePassed) {
            throw new PlazaLetterException("PLAZA409_ALREADY_GAVE_UP", "24시간이 지나 답장할 수 없어요");
        }

        // 5) 이미 답장했는지 체크
        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            throw new PlazaLetterException("PLAZA409_ALREADY_REPLIED", "이미 답장한 편지예요");
        }

        // 6) AI 검사(지금은 스텁으로 구현)
        // 실패 시: PLAZA400_AI_MODERATION_FAILED + 메시지 예시 "비속어/모욕적으로 표현된 의심 문장이 있어요"
        if (!passesModeration(text)) {
            throw new PlazaLetterException("PLAZA400_AI_MODERATION_FAILED", "비속어/모욕적으로 표현된 의심 문장이 있어요");
        }

        // 7) 답장 저장
        plazaLetterReplyRepository.save(PlazaLetterReply.builder()
                .letterId(letter.getLetterId())
                .replierId(userId)
                .text(text)
                .createdAt(now)
                .build());

        // 8) 편지 상태 업데이트
        // ARRIVED/DEFERRED → REPLIED
        updateLetterAsReplied(letter, now);

        // 9) rewards 구성(명세: INK 1 + SINCERITY 1)
        List<ReplyResponse.RewardDto> rewards = List.of(
                ReplyResponse.RewardDto.builder().kind(ReplyResponse.RewardKind.INK).amount(1).toastMessage(null).build(),
                ReplyResponse.RewardDto.builder().kind(ReplyResponse.RewardKind.SINCERITY).amount(1).toastMessage(null).build()
        );

        return ReplyResponse.builder()
                .letterId(letter.getLetterId())
                .status(PlazaLetterStatus.REPLIED)
                .repliedAt(now)
                .rewards(rewards)
                .build();
    }

    private void updateLetterAsReplied(PlazaLetter letter, OffsetDateTime now) {
        // 엔티티가 setter 없으면 "변경 가능한 메서드"를 만들어야 함.
        // Lombok @Getter만 쓰는 구조라면 아래처럼 엔티티에 메서드 하나 추가하는 게 깔끔함.
        letter.markReplied(now);
        // JPA dirty checking으로 자동 반영
    }

    private boolean passesModeration(String text) {
        // TODO: 나중에 진짜 AI 검사(80% 기준) 붙일 자리
        // 지금은 초간단 키워드 필터로 스텁
        String lowered = text.toLowerCase();
        String[] banned = {"시발", "ㅅㅂ", "병신", "멍청", "죽어", "꺼져"};
        for (String b : banned) {
            if (lowered.contains(b)) return false;
        }
        return true;
    }



}
