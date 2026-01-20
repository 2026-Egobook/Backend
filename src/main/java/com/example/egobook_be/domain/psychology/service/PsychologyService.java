package com.example.egobook_be.domain.psychology.service;

import com.example.egobook_be.domain.psychology.dto.*;
import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import com.example.egobook_be.domain.psychology.entity.UserKnowledge;
import com.example.egobook_be.domain.psychology.repository.PsychologyKnowledgeRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PsychologyService {

    private final PsychologyKnowledgeRepository psychologyKnowledgeRepository;
    private final UserKnowledgeRepository userKnowledgeRepository;
    private final UserRepository userRepository;
    private final InkLogRepository inkLogRepository;

    @Transactional
    public DailyKnowledgeResDto getDailyKnowledge(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        List<Long> savedIds = userKnowledgeRepository.findAllByUserAndDeletedAtIsNull(user)
                .stream()
                .map(uk -> uk.getPsychologyKnowledge().getId())
                .toList();

        List<PsychologyKnowledge> available = psychologyKnowledgeRepository.findAll().stream()
                .filter(pk -> !savedIds.contains(pk.getId()))
                .toList();

        if (available.isEmpty()) available = psychologyKnowledgeRepository.findAll();

        long seed = userId + LocalDate.now().toEpochDay();
        Random random = new Random(seed);
        PsychologyKnowledge todayKnowledge = available.get(random.nextInt(available.size()));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        boolean alreadyReceived = inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(
                user, InkLogType.FIRST_PSYCHOLOGY_VIEW, startOfToday);

        RewardInfoResDto reward = null;
        if (!alreadyReceived) {
            user.addInk(1);
            inkLogRepository.save(new InkLog(user, 1, InkLogType.FIRST_PSYCHOLOGY_VIEW));
            reward = new RewardInfoResDto(true, 1, user.getInk(), "잉크를 1 획득했어요");
        }

        KnowledgeInfoResDto knowledgeInfo = new KnowledgeInfoResDto(
                todayKnowledge.getId(), "오늘의 심리 지식", todayKnowledge.getContent(), todayKnowledge.getSource());

        return new DailyKnowledgeResDto(LocalDate.now().toString(), knowledgeInfo, reward);
    }

    @Transactional
    public KnowledgeSaveResDto saveKnowledge(Long userId, Long knowledgeId) {
        User user = userRepository.findById(userId).orElseThrow();
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(knowledgeId).orElseThrow();

        userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)
                .ifPresentOrElse(
                        uk -> { if (uk.getDeletedAt() != null) uk.restore(); },
                        () -> userKnowledgeRepository.save(new UserKnowledge(user, knowledge))
                );

        return new KnowledgeSaveResDto(true, knowledgeId, "오늘의 심리지식이 저장되었습니다.");
    }

    @Transactional
    public KnowledgeDeleteResDto deleteSavedKnowledge(Long userId, Long knowledgeId) {
        User user = userRepository.findById(userId).orElseThrow();
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(knowledgeId).orElseThrow();
        UserKnowledge userKnowledge = userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge).orElseThrow();

        userKnowledge.delete();
        return new KnowledgeDeleteResDto(true, knowledgeId, "북마크가 취소되었습니다.");
    }

    public SavedKnowledgeListResDto getSavedKnowledgeList(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<SavedKnowledgeListResDto.SavedKnowledgeItemResDto> items = userKnowledgeRepository.findAllByUserAndDeletedAtIsNull(user).stream()
                .map(uk -> new SavedKnowledgeListResDto.SavedKnowledgeItemResDto(
                        uk.getPsychologyKnowledge().getId(),
                        "오늘의 심리 지식",
                        uk.getPsychologyKnowledge().getContent().substring(0, Math.min(uk.getPsychologyKnowledge().getContent().length(), 20)) + "...",
                        uk.getSavedAt().format(DateTimeFormatter.ISO_DATE_TIME)))
                .toList();

        return new SavedKnowledgeListResDto(items, false, null);
    }

    public DailyStatusResDto getDailyStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        boolean alreadyReceived = inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(
                user, InkLogType.FIRST_PSYCHOLOGY_VIEW, startOfToday);

        return new DailyStatusResDto(!alreadyReceived);
    }
}