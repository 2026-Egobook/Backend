package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.MyRestrictionResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionInfoResDto;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.mapper.RestrictionMapper;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRestrictionService {

    private final RestrictionRepository restrictionRepository;
    private final RestrictionMapper restrictionMapper;

    /**
     * 로그인한 사용자의 도메인별(편지 / 오늘의 질문 답변) 현재 제재 상태 조회
     * - ACTIVE 이면서 제재 종료 시각이 아직 지나지 않은 제재만 "제재 중"으로 본다.
     * - 배치가 EXPIRED 로 갱신하기 전의 만료된 제재는 제재 중으로 보지 않는다.
     * - 같은 도메인에 제재가 여러 건이면 종료 시각이 가장 늦은 제재를 기준으로 한다.
     * @param userId : 조회할 사용자 ID
     * @return : 도메인별 제재 상태를 담은 MyRestrictionResDto
     */
    // 내 제재 상태 조회 로직
    @Transactional(readOnly = true)
    public MyRestrictionResDto getMyRestrictions(Long userId) {
        log.info("[UserRestrictionService] getMyRestrictions() - START | userId: {}", userId);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        RestrictionInfoResDto letter = findActiveRestrictionInfo(userId, RestrictionDomainType.LETTER, now);
        RestrictionInfoResDto questionAnswer = findActiveRestrictionInfo(userId, RestrictionDomainType.QUESTION_ANSWER, now);
        MyRestrictionResDto result = restrictionMapper.toMyRestrictionResDto(letter, questionAnswer);

        log.info("[UserRestrictionService] getMyRestrictions() - END | userId: {}, letterRestricted: {}, questionAnswerRestricted: {}",
                userId, letter.restricted(), questionAnswer.restricted());
        return result;
    }

    /**
     * 특정 도메인의 현재 유효한 제재를 조회해 응답 DTO로 변환
     * @param userId     : 조회할 사용자 ID
     * @param domainType : 제재 도메인 (LETTER | QUESTION_ANSWER)
     * @param now        : 기준 시각 (한국 시간)
     * @return : 제재 중이면 제재 정보, 아니면 restricted=false 인 DTO
     */
    // 도메인별 유효 제재 조회
    private RestrictionInfoResDto findActiveRestrictionInfo(Long userId, RestrictionDomainType domainType, LocalDateTime now) {
        return restrictionRepository
                .findFirstByUserIdAndDomainTypeAndStatusAndRestrictionUntilAfterOrderByRestrictionUntilDesc(
                        userId, domainType, RestrictionStatus.ACTIVE, now)
                .map(restrictionMapper::toInfoResDto)
                .orElseGet(restrictionMapper::toUnrestrictedInfoResDto);
    }
}
