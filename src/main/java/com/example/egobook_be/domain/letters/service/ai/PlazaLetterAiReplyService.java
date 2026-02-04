package com.example.egobook_be.domain.letters.service.ai;

import com.example.egobook_be.domain.letters.entity.*;
import com.example.egobook_be.domain.letters.repository.PlazaLetterReplyRepository;
import com.example.egobook_be.domain.letters.repository.PlazaLetterRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlazaLetterAiReplyService {

    private static final int AI_REPLY_CHAR_LIMIT = 360;

    private final PlazaLetterRepository plazaLetterRepository;
    private final PlazaLetterReplyRepository plazaLetterReplyRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;

    @Transactional
    public int generateAiReplies(int batchSize) {
        //OffsetDateTime cutoff = OffsetDateTime.now().minusHours(48);
        OffsetDateTime cutoff = OffsetDateTime.now().plusMinutes(1);  // 1분 뒤로 설정


        List<PlazaLetter> targets = plazaLetterRepository.findAiReplyTargets(
                cutoff,
                PlazaLetterStatus.REPLIED,
                PlazaLetterStatus.AI_REPLIED,
                PageRequest.of(0, batchSize)
        );

        int processed = 0;
        for (PlazaLetter letter : targets) {
            if (generateAiReplyIfEligible(letter.getLetterId())) {
                processed++;
            }
        }
        return processed;
    }

    @Transactional
    public boolean generateAiReplyIfEligible(Long letterId) {
        PlazaLetter letter = plazaLetterRepository.findById(letterId).orElse(null);
        if (letter == null) return false;


        if (letter.getStatus() == PlazaLetterStatus.REPLIED || letter.getStatus() == PlazaLetterStatus.AI_REPLIED) {
            return false;
        }

        // reply 존재하면 스킵
        if (plazaLetterReplyRepository.existsByLetterId(letterId)) {
            return false;
        }

        // 48시간 지났는지 최종 확인 (스케줄러 지연/수동 호출 대비)
        OffsetDateTime createdAt = letter.getCreatedAt();
        //if (createdAt == null || OffsetDateTime.now().isBefore(createdAt.plusHours(48))) {
        if (createdAt == null || OffsetDateTime.now().isBefore(createdAt.plusMinutes(1))) {
            return false;
        }

        String nickname = userRepository.findById(letter.getSenderId())
                .map(u -> u.getNickname())
                .orElse("");

        String raw = geminiClient.generateReply(nickname, letter.getContent());
        String normalized = normalizeAiReply(raw);

        OffsetDateTime now = OffsetDateTime.now();

        PlazaLetterReply aiReply = PlazaLetterReply.builder()
                .threadId(letter.getThreadId())
                .letterId(letter.getLetterId())
                .replierId(letter.getSenderId())
                .text(normalized)
                .isAiGenerated(true)
                .createdAt(now)
                .build();

        try {
            plazaLetterReplyRepository.save(aiReply);
        } catch (DataIntegrityViolationException e) {
            return false;
        }


        letter.markAiReplied(now);

        return true;
    }

    private String normalizeAiReply(String text) {
        if (text == null) return fallbackText();

        String t = text.trim();

        // 360자 컷
        if (t.length() > AI_REPLY_CHAR_LIMIT) {
            t = t.substring(0, AI_REPLY_CHAR_LIMIT).trim();
        }

        // 5줄 초과 컷
        String[] lines = t.split("\\r?\\n");
        if (lines.length > 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i > 0) sb.append("\n");
                sb.append(lines[i].trim());
            }
            t = sb.toString().trim();
        }

        // 3줄 미만이면 줄바꿈 보정
        lines = t.split("\\r?\\n");
        if (lines.length < 3) {
            t = forceLineBreaks(t);
        }

        if (t.isBlank()) return fallbackText();

        // 혹시라도 "당신/너" 등 금칙어가 들어가면 아주 간단하게 치환(강제)
        t = t.replace("당신", "")
                .replace("너", "")
                .replace("그대", "")
                .replace("여러분", "")
                .trim();

        if (t.isBlank()) return fallbackText();
        return t;
    }

    private String forceLineBreaks(String t) {

        String x = t.replaceAll("([.!?。])\\s+", "$1\n").trim();
        String[] lines = x.split("\\r?\\n");
        if (lines.length >= 3) return x;


        if (!x.contains("\n") && x.length() > 80) {
            int cut1 = Math.min(50, x.length());
            int cut2 = Math.min(100, x.length());
            String a = x.substring(0, cut1).trim();
            String b = x.substring(cut1, cut2).trim();
            String c = x.substring(cut2).trim();
            String merged = (a + "\n" + b + (c.isBlank() ? "" : "\n" + c)).trim();

            // 5줄 넘어가면 컷
            String[] mLines = merged.split("\\r?\\n");
            if (mLines.length > 5) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(mLines[i].trim());
                }
                return sb.toString().trim();
            }
            return merged;
        }

        return x;
    }

    private String fallbackText() {
        return "마음이 많이 무거운 시간을 지나고 있었겠어요.\n지금까지 버텨온 것만으로도 충분히 애쓰고 있어요.\n오늘은 마음이 조금이라도 가벼워졌으면 좋겠어요.";
    }
}

