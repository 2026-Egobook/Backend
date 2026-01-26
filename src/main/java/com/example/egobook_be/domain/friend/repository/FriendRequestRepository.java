package com.example.egobook_be.domain.friend.repository;

import com.example.egobook_be.domain.friend.entity.FriendRequest;
import com.example.egobook_be.domain.friend.enums.FriendRequestStatus;
import com.example.egobook_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByReceiverAndStatus(
            User receiver,
            FriendRequestStatus status
    );

    @Query("""
        select fr
        from FriendRequest fr
        join fetch fr.receiver r
        where fr.sender = :sender
          and fr.status = :status
    """)
    List<FriendRequest> findBySenderAndStatusWithReceiver(
            @Param("sender") User sender,
            @Param("status") FriendRequestStatus status
    );
}
