package com.example.egobook_be.domain.restriction.repository;

import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestrictionRepository extends JpaRepository<Restriction, Long> {

    long countAllByUserId(Long userId);

    boolean existsByUserIdAndDomainTypeAndStatus(Long userId, RestrictionDomainType domainType, RestrictionStatus status);

    // [AI-GEN] 사용자 전체 제재 기록 Slice 조회 (status 필터 없음)
    Slice<Restriction> findAllByUserId(Long userId, Pageable pageable);

    // [AI-GEN] 사용자 제재 기록 Slice 조회 (status 필터)
    Slice<Restriction> findAllByUserIdAndStatus(Long userId, RestrictionStatus status, Pageable pageable);
}
