package com.example.egobook_be.domain.shop.repository;

import com.example.egobook_be.domain.shop.entity.Item;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ItemRepository extends JpaRepository<Item, Long> {
    Slice<Item> findByCategory(ItemCategory category, Pageable pageable);

    List<Item> findAllByName(String name);

    boolean existsByNameAndPath(String name, String path);

    // 편지지 아이템 조회 (카테고리 + 색상명으로 조회)
    Optional<Item> findByCategoryAndName(ItemCategory category, String name);

}
