package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.mapper.RestrictionMapper;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRestrictionService {

    private final UserRepository userRepository;
    private final RestrictionRepository restrictionRepository;
    private final RestrictionMapper restrictionMapper;

    /**
     * 특정 사용자에게 7일간 제재를 적용한다.
     * @param adminId 제재를 수행하는 관리자 ID (감사 추적용)
     * @param userId  제재 대상 사용자 ID
     * @param reqDto  제재 요청 정보 (domainType, reason, description)
     * @return 생성된 제재 정보 (restrictionId, restrictionStatus, restrictionUntil)
     */
    @Transactional
    public RestrictionCreateResDto createRestriction(Long adminId, Long userId, RestrictionCreateReqDto reqDto) {
        log.info("[AdminRestrictionService] createRestriction() - START | adminId: {}, userId: {}, domainType: {}",
                adminId, userId, reqDto.domainType());

        // 1. 대상 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(RestrictionErrorCode.USER_NOT_FOUND));

        // 2. 동일 도메인 ACTIVE 제재 중복 확인
        if (restrictionRepository.existsByUserIdAndDomainTypeAndStatus(userId, reqDto.domainType(), RestrictionStatus.ACTIVE)) {
            throw new CustomException(RestrictionErrorCode.ALREADY_RESTRICTED);
        }

        // 3. 제재 엔티티 생성 및 저장
        Restriction restriction = Restriction.create(adminId, userId, reqDto.domainType(), reqDto.reason(), reqDto.description());
        Restriction saved = restrictionRepository.save(restriction);

        log.info("[AdminRestrictionService] createRestriction() - SUCCESS | restrictionId: {}, restrictionUntil: {}",
                saved.getRestrictionId(), saved.getRestrictionUntil());

        return restrictionMapper.toResDto(saved);
    }
}
