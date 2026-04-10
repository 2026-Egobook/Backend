package com.example.egobook_be.domain.restriction.repository;

import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RestrictionRepository extends JpaRepository<Restriction, Long> {

    long countAllByUserId(Long userId);

    boolean existsByUserIdAndDomainTypeAndStatus(Long userId, RestrictionDomainType domainType, RestrictionStatus status);

    // 사용자 전체 제재 기록 Slice 조회 (status 필터 없음)
    Slice<Restriction> findAllByUserId(Long userId, Pageable pageable);

    // 사용자 제재 기록 Slice 조회 (status 필터)
    Slice<Restriction> findAllByUserIdAndStatus(Long userId, RestrictionStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Restriction r
        SET r.status = :expired
        WHERE r.status = :active
        AND r.restrictionUntil < :now
        """)
    int bulkExpireOverdueRestrictions(
            @Param("expired") RestrictionStatus expired,
            @Param("active")  RestrictionStatus active,
            @Param("now")     LocalDateTime now
    );

    @Query("SELECT r.userId FROM Restriction r WHERE r.domainType = :domainType AND r.status = :status")
    List<Long> findUserIdsByDomainTypeAndStatus(
            @Param("domainType") RestrictionDomainType domainType,
            @Param("status")     RestrictionStatus status
    );
}
