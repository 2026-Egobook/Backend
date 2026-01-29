package com.example.egobook_be.domain.home.repository;

import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    /**
     * 사용자로 Mission을 찾는 함수 
     */
    Optional<Mission> findByUser(User user);
}
