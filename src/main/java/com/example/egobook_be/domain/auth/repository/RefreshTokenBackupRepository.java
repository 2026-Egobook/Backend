package com.example.egobook_be.domain.auth.repository;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenBackupRepository extends JpaRepository<AuthAccount, Long> {
}
