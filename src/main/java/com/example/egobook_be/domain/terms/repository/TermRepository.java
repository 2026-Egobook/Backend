package com.example.egobook_be.domain.terms.repository;

import com.example.egobook_be.domain.terms.entity.Term;
import com.example.egobook_be.domain.terms.enums.TermType;
import com.example.egobook_be.domain.terms.enums.TermVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Integer> {
    /**
     * TermType으로 해당 약관이 존재하는지 찾는 함수
     */
    Optional<Term> findByTermType(TermType termType);
}
