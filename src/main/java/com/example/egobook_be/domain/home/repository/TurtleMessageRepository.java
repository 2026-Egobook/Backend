package com.example.egobook_be.domain.home.repository;

import com.example.egobook_be.domain.home.entity.TurtleMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurtleMessageRepository extends JpaRepository<TurtleMessage, Long> {

    /**
     * 말풍선 문구 중 무작위로 1건 조회
     * - 문구가 수십 건 수준이라 ORDER BY RAND() 로 충분하다.
     * @return : 무작위 말풍선 문구 Entity (등록된 문구가 없으면 empty)
     */
    @Query(value = "SELECT * FROM turtle_message ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<TurtleMessage> findRandom();
}
