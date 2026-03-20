package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.InkLog;
import com.example.egobook_be.domain.user.entity.InkLogType;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InkLogRepository extends JpaRepository<InkLog, Long> {
    int countByUser(User user);
    
    boolean existsByUserAndReasonAndCreatedAtAfter(User user, InkLogType reason, LocalDateTime startOfDay);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InkLog i WHERE i.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}