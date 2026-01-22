package com.example.egobook_be.domain.shop.repository;

import com.example.egobook_be.domain.shop.entity.Item;

import com.example.egobook_be.domain.shop.enums.ItemCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ItemRepository extends JpaRepository<Item, Long> {
    Slice<Item> findByCategory(ItemCategory category, Pageable pageable);

}
