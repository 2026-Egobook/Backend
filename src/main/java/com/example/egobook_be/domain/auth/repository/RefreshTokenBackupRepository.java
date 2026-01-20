package com.example.egobook_be.domain.auth.repository;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.entity.RefreshTokenBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenBackupRepository extends JpaRepository<RefreshTokenBackup, Long> {

    /**
     * RefreshTokenBackup 테이블에 저장되어있는 Row 인스턴스를 AuthAccount PK로 가져오는 함수
     * @param authAccount
     * @return RefreshTokenBackup
     */
    Optional<RefreshTokenBackup> findByAuthAccount(AuthAccount authAccount);

    /**
     * 해당 테이블에 AuthAccount PK가 존재하는지 확인하는 함수
     * RefreshTokenBackup 테이블에서는 authAccount PK는 unique하다.
     * @param authAccount
     * @return
     */
    boolean existsByAuthAccount(AuthAccount authAccount);

    /**
     * hashedRefreshToken으로 해당 테이블에 존재하는 인스턴스 값을 가져오는 함수
     * @param hashedRefreshToken
     * @return
     */
    Optional<RefreshTokenBackup> findByHashedTokenValue(String hashedRefreshToken);
}
