package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.WithdrawReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawReasonRepository extends JpaRepository<WithdrawReason, Long> {
    boolean existsByUserId(Long userId);
}
