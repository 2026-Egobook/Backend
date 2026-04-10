package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;

public interface AdminRestrictionService {

    /**
     * 특정 사용자에게 7일간 제재를 적용한다.
     * @param adminId 제재를 수행하는 관리자 ID (감사 추적용)
     * @param userId  제재 대상 사용자 ID
     * @param reqDto  제재 요청 정보 (domainType, reason, description)
     * @return 생성된 제재 정보 (restrictionId, restrictionStatus, restrictionUntil)
     */
    RestrictionCreateResDto createRestriction(Long adminId, Long userId, RestrictionCreateReqDto reqDto);
}
