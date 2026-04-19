package com.example.egobook_be.domain.restriction.mapper;

import com.example.egobook_be.domain.restriction.dto.RestrictionCancelResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RestrictionMapper {

    /**
     * Restriction Entity -> RestrictionCreateResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @return 변환된 RestrictionCreateResDto
     */
    public RestrictionCreateResDto toResDto(Restriction restriction) {
        return RestrictionCreateResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .restrictionStatus(restriction.getStatus())
                .restrictionUntil(restriction.getRestrictionUntil())
                .build();
    }

    /**
     * Restriction Entity -> RestrictionItemResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @return 변환된 RestrictionItemResDto
     */
    public RestrictionItemResDto toItemResDto(Restriction restriction) {
        return RestrictionItemResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .domainType(restriction.getDomainType())
                .reason(restriction.getReason())
                .description(restriction.getDescription())
                .restrictionStatus(restriction.getStatus())
                .createdAt(restriction.getCreatedAt())
                .restrictionUntil(restriction.getRestrictionUntil())
                .build();
    }

    /**
     * Restriction Entity, User Entity -> RestrictionCancelResDto 변환
     * @param restriction 변환할 Restriction Entity
     * @param user 변환할 User Entity
     * @return 변환된 RestrictionCancelResDto
     */
    // [AI-GEN] 제재 해제 응답 매핑
    public RestrictionCancelResDto toCancelResDto(Restriction restriction, User user) {
        return RestrictionCancelResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .restrictionStatus(restriction.getStatus())
                .userId(user.getId())
                .userStatus(user.getStatus())
                .restrictionUntil(null)
                .build();
    }
}
