package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.Ability;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbilityRepository extends JpaRepository<Ability, Long> {
    Optional<Ability> findByUser(User user);
    void deleteByUserIn(List<User> users);

    @Query("select a from Ability a join fetch a.user where a.user.id in :userIds")
    List<Ability> findByUserIdIn(@Param("userIds") List<Long> userIds);
}
