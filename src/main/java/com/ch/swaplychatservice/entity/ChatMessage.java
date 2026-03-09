package com.ch.swaplychatservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.awt.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_room_created", columnList = "room_id, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type = MessageType.TEXT;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 정적 팩토리 메서드 ──────────────────────────────────
    public static ChatMessage create(ChatRoom room, Long senderId,
                                     String content, MessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.room     = room;
        msg.senderId = senderId;
        msg.content  = content;
        msg.type     = type;
        msg.isRead   = false;
        return msg;
    }

    public void markRead() {
        this.isRead = true;
    }
}
