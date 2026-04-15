package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (restrictionRepository.existsByUserIdAndDomainTypeAndStatus(
                userId, RestrictionDomainType.LETTER, RestrictionStatus.ACTIVE)) {
            throw new CustomException(RestrictionErrorCode.LETTER_RESTRICTED);
        }
    }

    /**
     * userId가 QUESTION_ANSWER 도메인 ACTIVE 제재 대상이면 403 예외를 발생시킨다.
     */
    @Transactional(readOnly = true)
    public void checkQuestionAnswerRestriction(Long userId) {
        if (restrictionRepository.existsByUserIdAndDomainTypeAndStatus(
                userId, RestrictionDomainType.QUESTION_ANSWER, RestrictionStatus.ACTIVE)) {
            throw new CustomException(RestrictionErrorCode.QUESTION_ANSWER_RESTRICTED);
        }
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
