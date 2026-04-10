package com.example.egobook_be.domain.restriction.mapper;

import com.example.egobook_be.domain.restriction.dto.RestrictionCreateResDto;
import com.example.egobook_be.domain.restriction.dto.RestrictionItemResDto;
import com.example.egobook_be.domain.restriction.entity.Restriction;
import org.springframework.stereotype.Component;

@Component
public class RestrictionMapper {

    public RestrictionCreateResDto toResDto(Restriction restriction) {
        return RestrictionCreateResDto.builder()
                .restrictionId(restriction.getRestrictionId())
                .restrictionStatus(restriction.getStatus())
                .restrictionUntil(restriction.getRestrictionUntil())
                .build();
    }

    // Restriction -> RestrictionItemResDto 변환
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
}
