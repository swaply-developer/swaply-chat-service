package com.ch.swaplychatservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_product_buyer",
                columnNames = {"product_id", "buyer_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    @Column(nullable = false)
    private int buyerUnread = 0;

    @Column(nullable = false)
    private int sellerUnread = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.ACTIVE;

    /**
     * 채팅에서 합의된 가격.
     * null = 원가 그대로 결제
     * non-null = 가격 제안 수락된 협의 가격
     */
    @Column(name = "negotiated_price")
    private Long negotiatedPrice;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    // ── 정적 팩토리 메서드 ──────────────────────────────────
    public static ChatRoom create(Long productId, Long buyerId, Long sellerId) {
        ChatRoom room = new ChatRoom();
        room.productId  = productId;
        room.buyerId    = buyerId;
        room.sellerId   = sellerId;
        room.status     = RoomStatus.ACTIVE;
        room.buyerUnread  = 0;
        room.sellerUnread = 0;
        return room;
    }

    // ── 도메인 메서드 ────────────────────────────────────────
    public void receiveMessage(String preview, Long senderId) {
        this.lastMessage = preview;
        if (senderId.equals(buyerId)) {
            this.sellerUnread++;
        } else {
            this.buyerUnread++;
        }
    }

    public void markRead(Long memberId) {
        if (memberId.equals(buyerId))   this.buyerUnread  = 0;
        if (memberId.equals(sellerId))  this.sellerUnread = 0;
    }

    public void close() {
        this.status = RoomStatus.CLOSED;
    }

    public boolean isParticipant(Long memberId) {
        return buyerId.equals(memberId) || sellerId.equals(memberId);
    }

    /** 가격 제안 수락 시 협의 가격 저장 */
    public void acceptNegotiatedPrice(Long price) {
        this.negotiatedPrice = price;
    }
}
