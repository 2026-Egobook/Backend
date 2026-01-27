package com.example.egobook_be.domain.terms.repository;

import com.example.egobook_be.domain.terms.entity.UserTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermRepository extends JpaRepository<UserTerm, Long> {
}
