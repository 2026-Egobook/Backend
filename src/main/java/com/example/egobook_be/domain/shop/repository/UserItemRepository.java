package com.example.egobook_be.domain.shop.repository;

import com.example.egobook_be.domain.shop.dto.UserItemStatusDto;
import com.example.egobook_be.domain.shop.entity.UserItem;
import com.example.egobook_be.domain.shop.enums.ItemCategory;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.domain.user.repository.InkLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    /**
     * UserItem Table에서 사용자가 구매한 아이템 목록을 Set으로 가져오는 함수
     * @param userId 사용자 PK
     * @param itemIds Item들의 PK가 담긴 List
     * @return
     */
    @Query("select new com.example.egobook_be.domain.shop.dto.UserItemStatusDto(ui.item.id, ui.isEquipped) " +
            "from UserItem ui " +
            "where ui.user.id = :userId and ui.item.id in :itemIds")
    Set<UserItemStatusDto> findUserItemStatusSetByItem(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

    /**
     * 해당 사용자가 이미 해당 id의 아이템을 구매했는지 확인하는 함수
     */
    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    @Query("select ui from UserItem ui join fetch ui.item " +
            "where ui.user.id = :userId and ui.item.id = :itemId")
    Optional<UserItem> findByUserIdAndItemId(@Param("userId") Long userId,
                                             @Param("itemId") Long itemId);

    @Query("select ui from UserItem ui join ui.item i " +
            "where ui.user.id = :userId and i.category = :category and ui.isEquipped = true")
    List<UserItem> findEquippedItemsByCategory(@Param("userId") Long userId,
                                               @Param("category") ItemCategory category);

    @Query("select ui from UserItem ui " +
            "join fetch ui.item " +
            "where ui.user.id = :userId and ui.isEquipped = true")
    List<UserItem> findEquippedItems(@Param("userId")Long userId);

    void deleteByUserIn(List<User> users);

    @Query(value = """
    SELECT DATE_FORMAT(ui.purchased_at, :format) AS period,
           SUM(i.price) AS amount
    FROM user_item ui
    JOIN item i ON ui.item_id = i.id
    WHERE ui.purchased_at >= :start
      AND ui.purchased_at < :end
      AND i.price IS NOT NULL
    GROUP BY period
    ORDER BY period
    """, nativeQuery = true)
    List<InkLogRepository.InkStatAmount> sumConsumed(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("format") String format
    );
}
