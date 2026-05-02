package com.example.egobook_be.domain.psychology.service;

import com.example.egobook_be.domain.psychology.dto.*;
import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import com.example.egobook_be.domain.psychology.entity.UserKnowledge;
import com.example.egobook_be.domain.psychology.exception.PsychologyErrorCode;
import com.example.egobook_be.domain.psychology.repository.PsychologyKnowledgeRepository;
import com.example.egobook_be.domain.psychology.repository.UserKnowledgeRepository;
import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
@Slf4j
@Service
@RequiredArgsConstructor
public class PsychologyService {

    private final PsychologyKnowledgeRepository psychologyKnowledgeRepository;
    private final UserKnowledgeRepository userKnowledgeRepository;
    private final UserRepository userRepository;
    private final InkLogRepository inkLogRepository;

    @Transactional
    public DailyKnowledgeResDto getDailyKnowledge(Long userId) {
        log.info("[PsychologyService] getDailyKnowledge Start - userId: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        log.info("[PsychologyService] getDailyKnowledge End - userId: {}", userId);
        // 1. 오늘 이 유저가 이미 조회한 기록이 있는지 확인
        return userKnowledgeRepository.findFirstByUserAndCreatedAtAfter(user, startOfToday)
                .map(uk -> createResponse(user, uk.getPsychologyKnowledge(), uk.isBookmarked())) // 기존 기록 반환
                .orElseGet(() -> {
                    // 2. 오늘 처음 조회라면: 아직 유저지식 테이블에 없는(한 번도 안 본) 지식들 필터링
                    List<Long> viewedIds = userKnowledgeRepository.findAllByUser(user).stream()
                            .map(uk -> uk.getPsychologyKnowledge().getId())
                            .toList();

                    List<PsychologyKnowledge> candidates = psychologyKnowledgeRepository.findAllByDeletedAtIsNull().stream()
                            .filter(pk -> !viewedIds.contains(pk.getId()))
                            .toList();

                    if (candidates.isEmpty()) {
                        userKnowledgeRepository.deleteAllByUserId(userId);
                        candidates = psychologyKnowledgeRepository.findAllByDeletedAtIsNull();
                    }
                    // 3. 오늘 지식 확정 및 DB 저장
                    long seed = userId + LocalDate.now().toEpochDay();
                    PsychologyKnowledge picked = candidates.get(new Random(seed).nextInt(candidates.size()));

                    // 조회 기록 남기기
                    UserKnowledge newHistory = new UserKnowledge(user, picked);
                    newHistory.setBookmarked(false); // 처음엔 조회만 한 상태
                    userKnowledgeRepository.save(newHistory);

                    return createResponse(user, picked, false);
                });
    }

    @Transactional
    public KnowledgeSaveResDto saveKnowledge(Long userId, Long knowledgeId) {
        log.info("[PsychologyService] saveKnowledge Start - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.USER_NOT_FOUND));
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(knowledgeId)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.KNOWLEDGE_NOT_FOUND));

        userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)
                .ifPresentOrElse(
                        uk -> {
                            if (uk.isBookmarked() && uk.getDeletedAt() == null) {
                                throw new CustomException(PsychologyErrorCode.ALREADY_BOOKMARKED);
                            }
                            if (uk.getDeletedAt() != null) {
                                uk.restore();
                            } else {
                                uk.updateBookmarkStatus(true);
                            }
                        },
                        () -> {
                            UserKnowledge newUk = new UserKnowledge(user, knowledge);
                            newUk.updateBookmarkStatus(true);
                            userKnowledgeRepository.save(newUk);
                        }
                );

        log.info("[PsychologyService] saveKnowledge End - userId: {}", userId);
        return new KnowledgeSaveResDto(true, knowledgeId, "오늘의 심리지식이 저장되었습니다.");
    }

    @Transactional
    public KnowledgeDeleteResDto deleteSavedKnowledge(Long userId, Long knowledgeId) {
        log.info("[PsychologyService] deleteSavedKnowledge Start - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.USER_NOT_FOUND));
        PsychologyKnowledge knowledge = psychologyKnowledgeRepository.findById(knowledgeId)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.KNOWLEDGE_NOT_FOUND));

        UserKnowledge uk = userKnowledgeRepository.findByUserAndPsychologyKnowledge(user, knowledge)
                .orElseThrow(() -> new CustomException(PsychologyErrorCode.USER_KNOWLEDGE_NOT_FOUND));

        if (!uk.isBookmarked()) {
            throw new CustomException(PsychologyErrorCode.NOT_BOOKMARKED);
        }

        uk.delete();

        log.info("[PsychologyService] deleteSavedKnowledge End - userId: {}", userId);
        return new KnowledgeDeleteResDto(true, knowledgeId, "북마크가 취소되었습니다.");
    }

    @Transactional(readOnly = true)
    public SavedKnowledgeListResDto getSavedKnowledgeList(Long userId) {
        log.info("[PsychologyService] getSavedKnowledgeList Start - userId: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();

        List<SavedKnowledgeListResDto.SavedKnowledgeItemResDto> items = userKnowledgeRepository.findAllByUserAndDeletedAtIsNull(user).stream()
                .filter(UserKnowledge::isBookmarked)
                .map(uk -> new SavedKnowledgeListResDto.SavedKnowledgeItemResDto(
                        uk.getPsychologyKnowledge().getId(),
                        "오늘의 심리 지식",
                        uk.getPsychologyKnowledge().getContent(),
                        uk.getPsychologyKnowledge().getSource(),
                        uk.getSavedAt() != null ? uk.getSavedAt().format(DateTimeFormatter.ISO_DATE_TIME) : ""))
                .toList();

        log.info("[PsychologyService] getSavedKnowledgeList End - userId: {}", userId);
        return new SavedKnowledgeListResDto(items, false, null);
    }

    @Transactional(readOnly = true)
    public DailyStatusResDto getDailyStatus(Long userId) {
        log.info("[PsychologyService] getDailyStatus Start - userId: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        boolean alreadyReceived = inkLogRepository.existsByUserAndReasonAndCreatedAtAfter(
                user, InkLogType.FIRST_PSYCHOLOGY_VIEW, startOfToday);

        log.info("[PsychologyService] getDailyStatus End - userId: {}", userId);
        return new DailyStatusResDto(!alreadyReceived);
    }

    private DailyKnowledgeResDto createResponse(User user, PsychologyKnowledge knowledge, boolean isBookmarked) { LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); boolean alreadyReceived = inkLogRepository.existsByUserAndReasonAndCreatedAtAfter( user, InkLogType.FIRST_PSYCHOLOGY_VIEW, startOfToday);

        RewardInfoResDto reward = null;
        if (!alreadyReceived) {
            user.addInk(1);
            inkLogRepository.save(new InkLog(user, 1, InkLogType.FIRST_PSYCHOLOGY_VIEW));
            reward = new RewardInfoResDto(true, 1, user.getInk(), "잉크를 1 획득했어요");
        }

        KnowledgeInfoResDto knowledgeInfo = new KnowledgeInfoResDto(
                knowledge.getId(), "오늘의 심리 지식", knowledge.getContent(), knowledge.getSource());

        return new DailyKnowledgeResDto(LocalDate.now().toString(), knowledgeInfo, reward, isBookmarked);
    }
}