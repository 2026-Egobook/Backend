package com.example.egobook_be.domain.ego_room.repository;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.ego_room.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    Optional<UserStats> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserStats x WHERE x.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}
