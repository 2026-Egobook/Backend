package com.example.egobook_be.domain.friend.repository;

import com.example.egobook_be.domain.friend.entity.FriendRequest;
import com.example.egobook_be.domain.friend.enums.FriendRequestStatus;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySenderAndReceiverAndStatus(
            User sender,
            User receiver,
            FriendRequestStatus status
    );

    Optional<FriendRequest> findByIdAndReceiver(Long id, User receiver);

    Optional<FriendRequest> findByIdAndSender(Long id, User sender);

    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequestStatus status);

    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequestStatus status);
}
