package com.example.egobook_be.domain.auth.repository;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    /**
     * deviceUid와 provider를 동시에 조건으로 걸어서 유니크한 1개의 AuthAccount 객체를 반환하는 함수
     * - N+1 문제 방지를 위해, User 정보까지 한 번에 가져오는 Fetch Join을 적용하였음
     * @param deviceUid : 접속한 기기의 UID
     * @param provider : 접속 제공자
     * @return
     */
    @Query("SELECT a FROM AuthAccount a JOIN FETCH a.user " +
            "WHERE a.deviceUid = :deviceUid AND a.provider = :provider")
    Optional<AuthAccount> findByDeviceUidAndProvider(
            @Param("deviceUid") String deviceUid,
            @Param("provider") Provider provider
    );
}
