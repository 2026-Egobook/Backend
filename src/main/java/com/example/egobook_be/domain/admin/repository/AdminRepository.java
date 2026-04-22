package com.example.egobook_be.domain.admin.repository;

import com.example.egobook_be.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    boolean existsByAdminId(String adminId);

    Optional<Admin> findByAdminId(String adminId);
}
