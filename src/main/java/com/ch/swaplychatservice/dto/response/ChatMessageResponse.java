package com.ch.swaplychatservice.dto.response;

import com.ch.swaplychatservice.entity.ChatMessage;
import com.ch.swaplychatservice.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long   messageId;
    private Long   roomId;
    private Long   senderId;
    private String senderNickname;
    private String senderProfileImage;
    private String content;
    private MessageType type;
    private boolean isRead;
    private LocalDateTime createdAt;

    // ── 닉네임/프로필 없이 엔티티만으로 변환 (캐시 미스 시)
    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .messageId(msg.getMessageId())
                .roomId(msg.getRoom().getRoomId())
                .senderId(msg.getSenderId())
                .content(msg.getContent())
                .type(msg.getType())
                .isRead(msg.isRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    // ── 발신자 정보 포함 변환
    public static ChatMessageResponse of(ChatMessage msg,
                                         String senderNickname,
                                         String senderProfileImage) {
        return ChatMessageResponse.builder()
                .messageId(msg.getMessageId())
                .roomId(msg.getRoom().getRoomId())
                .senderId(msg.getSenderId())
                .senderNickname(senderNickname)
                .senderProfileImage(senderProfileImage)
                .content(msg.getContent())
                .type(msg.getType())
                .isRead(msg.isRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
