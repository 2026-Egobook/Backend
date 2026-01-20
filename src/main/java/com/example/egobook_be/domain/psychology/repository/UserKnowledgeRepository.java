package com.example.egobook_be.domain.psychology.repository;


import com.example.egobook_be.domain.psychology.entity.UserKnowledge;
import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserKnowledgeRepository extends JpaRepository<UserKnowledge, Long> {
    List<UserKnowledge> findAllByUserAndDeletedAtIsNull(User user);
    Optional<UserKnowledge> findByUserAndPsychologyKnowledge(User user, PsychologyKnowledge psychologyKnowledge);
}