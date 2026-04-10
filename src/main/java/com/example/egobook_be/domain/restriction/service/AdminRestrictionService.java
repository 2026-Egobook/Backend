package com.example.egobook_be.domain.restriction.service;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateReqDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import com.example.egobook_be.domain.restriction.exception.RestrictionErrorCode;
import com.example.egobook_be.domain.restriction.mapper.RestrictionMapper;
import com.example.egobook_be.domain.restriction.repository.RestrictionRepository;
import com.example.egobook_be.domain.user.repository.UserRepository;
import com.example.egobook_be.global.exception.CustomException;
import com.example.egobook_be.global.exception.GlobalErrorCode;
import com.example.egobook_be.global.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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

    /**
     * 특정 사용자의 제재 기록 목록을 페이지네이션으로 조회한다.
     * - status가 null이면 전체 조회, 값이 있으면 해당 상태로 필터링한다.
     * - createdAt 내림차순으로 정렬한다.
     * @param userId 조회 대상 사용자 ID
     * @param page   페이지 번호 (1~N, 프론트 기준)
     * @param size   페이지 크기 (1~100)
     * @param status 제재 상태 필터 (null 허용 시 전체 조회)
     * @return SliceResponse<RestrictionItemResDto>
     */
    // [AI-GEN] 사용자 제재 기록 목록 조회 메서드
    @Transactional(readOnly = true)
    public SliceResponse<RestrictionItemResDto> getRestrictionList(
            Long userId, int page, int size, RestrictionStatus status) {
        log.info("[AdminRestrictionService] getRestrictionList() - START | userId: {}, page: {}, size: {}, status: {}",
                userId, page, size, status);

        // [AI-GEN] page/size 입력값 검증
        if (page < 1) throw new CustomException(GlobalErrorCode.INVALID_SLICE_VALUE);
        if (size < 1 || size > 100) throw new CustomException(GlobalErrorCode.INVALID_SIZE_VALUE);

        // [AI-GEN] 대상 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(RestrictionErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // [AI-GEN] status nullable 분기 조회
        Slice<Restriction> slice = (status == null)
                ? restrictionRepository.findAllByUserId(userId, pageable)
                : restrictionRepository.findAllByUserIdAndStatus(userId, status, pageable);

        SliceResponse<RestrictionItemResDto> result = SliceResponse.of(slice, restrictionMapper::toItemResDto);

        log.info("[AdminRestrictionService] getRestrictionList() - END | userId: {}, size: {}",
                userId, result.size());
        return result;
    }
}
