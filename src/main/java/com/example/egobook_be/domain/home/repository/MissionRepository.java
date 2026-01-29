package com.example.egobook_be.domain.home.repository;

import com.example.egobook_be.domain.home.entity.Mission;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    /**
     * 사용자로 Mission을 찾는 함수 
     */
    Optional<Mission> findByUser(User user);

    /**
     * [성능 최적화] 모든 미션의 일일 수행 상태를 false로 초기화 (Bulk Update)
     * - @Modifying: JPA의 Repository 메서드는 기본적으로 select를 가정하고 있는데, 이 어노테이션은 해당 버전을 UPDATE로 바꿔준다.
     * - JPA Dirty Checking을 사용하면 메모리 낭비가 있을 수 있으므로, 영속성 컨텍스트를 무시하고 DB에 바로 쿼리를 날린다.
     * - clearAutomatically = true: 쿼리 실행 후 영속성 컨텍스트를 비워서 데이터 불일치 방지
     */
    @Modifying(clearAutomatically = true)
    @Query("update Mission m " +
            "set " +
            "m.dailyMissionSuccess = false, " +
            "m.dailyDiaryWritten = false, " +
            "m.dailyLetterWritten = false, " +
            "m.dailyQuestionAnswered = false ")
    void resetAllDailyMissions();
}
