package com.example.egobook_be.domain.ads.repository;

import com.example.egobook_be.domain.ads.entity.Ads;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdsRepository extends JpaRepository<Ads, Long> {
}
