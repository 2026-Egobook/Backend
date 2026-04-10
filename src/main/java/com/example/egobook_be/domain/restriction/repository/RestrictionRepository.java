package com.example.egobook_be.domain.restriction.repository;

import com.example.egobook_be.domain.restriction.entity.Restriction;
import com.example.egobook_be.domain.restriction.enums.RestrictionDomainType;
import com.example.egobook_be.domain.restriction.enums.RestrictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestrictionRepository extends JpaRepository<Restriction, Long> {

    long countAllByUserId(Long userId);

    boolean existsByUserIdAndDomainTypeAndStatus(Long userId, RestrictionDomainType domainType, RestrictionStatus status);
}
