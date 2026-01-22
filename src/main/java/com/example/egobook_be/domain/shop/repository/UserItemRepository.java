package com.example.egobook_be.domain.shop.repository;

import com.example.egobook_be.domain.shop.dto.UserItemStatusDto;
import com.example.egobook_be.domain.shop.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    /**
     * UserItem Table에서 사용자가 구매한 아이템 목록을 Set으로 가져오는 함수
     * @param userId 사용자 PK
     * @param itemIds Item들의 PK가 담긴 List
     * @return
     */
    @Query("select new com.example.egobook_be.domain.shop.dto.UserItemStatusDto(ui.id, ui.isEquipped) " +
            "from UserItem ui " +
            "where ui.user.id = :userId and ui.item.id in :itemIds")
    Set<UserItemStatusDto> findUserItemIdSetByItem(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

}
