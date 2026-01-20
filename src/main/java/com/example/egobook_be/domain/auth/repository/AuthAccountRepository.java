package com.example.egobook_be.domain.auth.repository;

import com.example.egobook_be.domain.auth.entity.AuthAccount;
import com.example.egobook_be.domain.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    /**
     * deviceUid와 provider를 동시에 조건으로 걸어서 유니크한 1개의 AuthAccount 객체를 반환하는 함수
     * - N+1 문제 방지를 위해, User 정보까지 한 번에 가져오는 Fetch Join을 적용하였음
     * @param hashedDeviceUid : 접속한 기기의 UID
     * @param provider : 접속 제공자
     * @return
     */
    @Query("SELECT a FROM AuthAccount a JOIN FETCH a.user " +
            "WHERE a.hashedDeviceUid = :hashedDeviceUid AND a.provider = :provider")
    Optional<AuthAccount> findByDeviceUidAndProvider(
            @Param("hashedDeviceUid") String hashedDeviceUid,
            @Param("provider") Provider provider
    );
    /**
     * HashedDeviceUid & Provider로 AuthAccount 객체를 찾는 함수
     * @param hashedDeviceUid String
     * @param provider Provider
     * @return Optional<AuthAccount></AuthAccount>
     */
    Optional<AuthAccount> findByHashedDeviceUidAndProvider(String hashedDeviceUid, Provider provider);
    /**
     * 특정 유저의 특정 Provider 계정 조회 (Guest 계정 찾기용)
     */
    Optional<AuthAccount> findByUserIdAndProvider(Long userId, Provider provider);

    /**
     * DeviceUid & Provider로 해당 인증 정보가 존재하는지 찾는 함수 (중복 가입 방지용)
     * @param hashedDeviceUid : 기기 고유 Uid
     * @param provider : 요청 제공자 GUEST, GOOGLE
     * @return
     */
    boolean existsByHashedDeviceUidAndProvider(String hashedDeviceUid, Provider provider);
}
