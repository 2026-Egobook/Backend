package com.example.egobook_be.domain.friend.repository;


import com.example.egobook_be.domain.friend.entity.Friend;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    boolean existsByUserAndFriend(User user, User friend);

    void deleteByUserAndFriend(User user, User friend);

    List<Friend> findByUser(User user);
}
