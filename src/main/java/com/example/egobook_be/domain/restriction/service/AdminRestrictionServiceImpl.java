package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRestrictionServiceImpl implements AdminRestrictionService {

    /**
     * 특정 사용자에게 7일간 제재를 적용한다.
     * - TODO: Restriction Entity / Repository / Mapper 구현 후 교체
     * @param adminId 제재를 수행하는 관리자 ID (감사 추적용)
     * @param userId  제재 대상 사용자 ID
     * @param reqDto  제재 요청 정보 (domainType, reason, description)
     * @return 생성된 제재 정보 (restrictionId, restrictionStatus, restrictionUntil)
     */
    @Override
    public RestrictionCreateResDto createRestriction(Long adminId, Long userId, RestrictionCreateReqDto reqDto) {
        log.info("[AdminRestrictionService] createRestriction() - START | adminId: {}, userId: {}, domainType: {}",
                adminId, userId, reqDto.domainType());

        // TODO: Restriction 도메인 Entity/Repository 구현 후 실제 로직으로 교체
        throw new UnsupportedOperationException("Restriction 도메인 구현 전입니다. Entity/Repository/Mapper 구현 후 교체하세요.");
    }
}
