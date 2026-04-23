package com.example.egobook_be.domain.stat.repository;

import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Repository
public interface AdminStatRepository extends Repository<User, Long> {

    @Query(value = """
    SELECT DATE_FORMAT(u.created_at, :format) AS period, COUNT(u.id) AS count
    FROM User u
    WHERE u.created_at >= :start 
      AND u.created_at < :end
    GROUP BY DATE_FORMAT(u.created_at, :format)
    ORDER BY period
    """, nativeQuery = true)
    List<JoinWithdrawCount> countJoin(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("format") String format
    );

    @Query(value = """
    SELECT DATE_FORMAT(u.deleted_at, :format) AS period, COUNT(u.id) AS count
    FROM User u
    WHERE u.deleted_at >= :start 
      AND u.deleted_at < :end
    GROUP BY DATE_FORMAT(u.deleted_at, :format)
    ORDER BY period
    """, nativeQuery = true)
    List<JoinWithdrawCount> countWithdraw(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("format") String format
    );

    interface JoinWithdrawCount {
        String getPeriod();
        Long getCount();
    }
}
