package com.ch.swaplychatservice.controller;

import com.ch.swaplychatservice.dto.response.StompMessagePayload;
import com.ch.swaplychatservice.entity.MessageType;
import com.ch.swaplychatservice.service.ChatMessageService;
import com.ch.swaplychatservice.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 STOMP 메시지를 보낼 때
     * destination: /app/chat.message
     *
     * payload: { roomId, content, type }
     * → 저장 후 /topic/chat.{roomId} 로 브로드캐스트
     */
    @MessageMapping("/chat.message")
    public void handleMessage(
            @Payload StompMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // STOMP 세션에서 memberId 꺼내기 (HandshakeInterceptor 에서 설정됨)
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) {
            log.warn("[WS] 세션 속성 없음 — 인증 실패");
            return;
        }

        Long senderId = (Long) sessionAttrs.get("memberId");
        if (senderId == null) {
            log.warn("[WS] senderId 없음 — 인증되지 않은 연결");
            return;
        }

        Long        roomId  = payload.getRoomId();
        String      content = payload.getContent();
        MessageType type    = payload.getType() != null ? payload.getType() : MessageType.TEXT;

        if (roomId == null || content == null || content.isBlank()) {
            log.warn("[WS] 잘못된 메시지 페이로드: roomId={}, content={}", roomId, content);
            return;
        }

        try {
            chatMessageService.sendMessage(roomId, senderId, content, type);
        } catch (SecurityException e) {
            log.warn("[WS] 권한 없음: sender={}, room={}", senderId, roomId);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/error",
                    Map.of("error", "채팅방 접근 권한이 없습니다")
            );
        } catch (Exception e) {
            log.error("[WS] 메시지 처리 실패", e);
        }
    }

    /**
     * 타이핑 표시
     * destination: /app/chat.typing
     * payload: { roomId, isTyping }
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) return;

        Long senderId = (Long) sessionAttrs.get("memberId");
        if (senderId == null) return;

        Long    roomId   = Long.parseLong(String.valueOf(payload.get("roomId")));
        boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));

        messagingTemplate.convertAndSend(
                "/topic/chat." + roomId + ".typing",
                Map.of("senderId", senderId, "isTyping", isTyping)
        );
    }
}
