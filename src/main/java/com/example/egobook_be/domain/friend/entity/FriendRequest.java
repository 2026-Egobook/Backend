package com.example.egobook_be.domain.friend.entity;

import com.example.egobook_be.domain.friend.enums.FriendRequestStatus;
import com.example.egobook_be.domain.user.entity.User;
import com.example.egobook_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "friend_request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friend_request_sender_receiver",
                        columnNames = {"sender_id", "receiver_id"}
                )
        }
)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 친구 신청 보낸 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** 친구 신청 받은 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = FriendRequestStatus.REJECTED;
    }
}
