package com.example.egobook_be.domain.terms.repository;

import com.example.egobook_be.domain.terms.entity.UserTerm;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTermRepository extends JpaRepository<UserTerm, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserTerm ut WHERE ut.user IN :users")
    void bulkDeleteByUserIn(@Param("users") List<User> users);
}
