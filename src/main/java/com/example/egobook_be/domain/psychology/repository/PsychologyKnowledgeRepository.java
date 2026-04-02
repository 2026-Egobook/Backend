package com.example.egobook_be.domain.psychology.repository;

import com.example.egobook_be.domain.psychology.entity.PsychologyKnowledge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PsychologyKnowledgeRepository extends JpaRepository<PsychologyKnowledge, Long> {

    Slice<PsychologyKnowledge> findAllByDeletedAtIsNull(Pageable pageable);
    List<PsychologyKnowledge> findAllByDeletedAtIsNull();


}