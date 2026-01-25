package com.example.egobook_be.domain.user.repository;

import com.example.egobook_be.domain.user.entity.Ability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbilityRepository extends JpaRepository<Ability, Long> {
}
