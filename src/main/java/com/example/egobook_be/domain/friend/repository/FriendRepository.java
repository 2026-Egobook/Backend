package com.example.egobook_be.domain.friend.repository;


import com.example.egobook_be.domain.friend.entity.Friend;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    boolean existsByUserAndFriend(User user, User friend);

    void deleteByUserAndFriend(User user, User friend);

    List<Friend> findByUser(User user);

    long countByUser(User user);

    @Query("""
        select f.friend.id
        from Friend f
        where f.user = :user
    """)
    List<Long> findFriendIdsByUser(@Param("user") User user);

    @Query("""
        select f
        from Friend f
        join fetch f.friend u
        where f.user = :user
    """)
    List<Friend> findByUserWithFriend(
            @Param("user") User user
    );
}
