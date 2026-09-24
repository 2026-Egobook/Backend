package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestrictionGuardService {

    private final RestrictionRepository restrictionRepository;

    /**
     * userId가 LETTER 도메인 ACTIVE 제재 대상이면 403 예외를 발생시킨다.
     */
    @Transactional(readOnly = true)
    public void checkLetterRestriction(Long userId) {
        if (getLetterRestrictionInfo(userId).restricted()) {
            throw new CustomException(RestrictionErrorCode.LETTER_RESTRICTED);
        }
    }

    /**
     * userId가 QUESTION_ANSWER 도메인 ACTIVE 제재 대상이면 403 예외를 발생시킨다.
     */
    @Transactional(readOnly = true)
    public void checkQuestionAnswerRestriction(Long userId) {
        if (restrictionRepository.existsByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfter(
                userId, RestrictionDomainType.QUESTION_ANSWER, RestrictionStatus.ACTIVE, now())) {
            throw new CustomException(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED);
        }
    }

    /** 프론트의 제한 안내 및 수신함 회색 처리용. 종료 시각은 기존 DB의 한국 시각 기준. */
    public record RestrictionInfo(boolean restricted, String reason, LocalDateTime restrictedUntil) {
        public static RestrictionInfo unrestricted() { return new RestrictionInfo(false, null, null); }
    }

    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Seoul")); }

    @Transactional(readOnly = true)
    public RestrictionInfo getLetterRestrictionInfo(Long userId) {
        return restrictionRepository
                .findFirstByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfterOrderByRestrictionUntilDesc(
                        userId, RestrictionDomainType.LETTER, RestrictionStatus.ACTIVE, now())
                .map(r -> new RestrictionInfo(true, r.getReason(), r.getRestrictionUntil()))
                .orElseGet(RestrictionInfo::unrestricted);
    }

    /**
     * 특정 도메인에서 ACTIVE 제재를 받고 있는 userId Set을 반환한다.
     * 수신자 풀 필터링 목적으로 사용한다 (contains() O(1)).
     */
    @Transactional(readOnly = true)
    public Set<Long> getActivelyRestrictedUserIds(RestrictionDomainType domainType) {
        return new HashSet<>(restrictionRepository.findUserIdsByDomainTypeAndStatus(
                domainType, RestrictionStatus.ACTIVE));
    }
}
