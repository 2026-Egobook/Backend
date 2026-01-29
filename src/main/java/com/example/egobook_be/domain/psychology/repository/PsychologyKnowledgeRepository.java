package com.example.egobook_be.domain.psychology.repository;


import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychologyKnowledgeRepository extends JpaRepository<PsychologyKnowledge, Long> {
}