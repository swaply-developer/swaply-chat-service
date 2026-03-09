package com.ch.swaplychatservice.service;

import com.ch.swaplychatservice.dto.response.ChatMessageResponse;
import com.ch.swaplychatservice.dto.response.MemberInfo;
import com.ch.swaplychatservice.dto.response.StompMessagePayload;
import com.ch.swaplychatservice.entity.ChatMessage;
import com.ch.swaplychatservice.entity.ChatRoom;
import com.ch.swaplychatservice.entity.MessageType;
import com.ch.swaplychatservice.repository.ChatMessageRepository;
import com.ch.swaplychatservice.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository    roomRepository;
    private final ChatMessageRepository messageRepository;
    private final MemberQueryService    memberQuery;
    private final SimpMessagingTemplate messagingTemplate;

    // ── 메시지 목록 조회 ────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long roomId, Long memberId, int page, int size) {
        // 방 접근 권한 확인
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));
        if (!room.isParticipant(memberId)) {
            throw new SecurityException("채팅방 접근 권한이 없습니다");
        }

        return messageRepository.findByRoomId(roomId, PageRequest.of(page, size))
                .map(msg -> {
                    MemberInfo sender = memberQuery.getMember(msg.getSenderId());
                    return ChatMessageResponse.of(msg, sender.getNickname(), sender.getProfileImage());
                });
    }

    // ── STOMP 메시지 전송 + 브로드캐스트 ───────────────────
    @Transactional
    public StompMessagePayload sendMessage(Long roomId, Long senderId,
                                           String content, MessageType type) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));
        if (!room.isParticipant(senderId)) {
            throw new SecurityException("채팅 권한 없음");
        }

        // 1. 메시지 DB 저장
        ChatMessage msg = ChatMessage.create(room, senderId, content, type);
        messageRepository.save(msg);

        // 2. 채팅방 마지막 메시지 + unread 업데이트
        String preview = buildPreview(content, type);
        room.receiveMessage(preview, senderId);

        // 3. 발신자 정보
        MemberInfo sender = memberQuery.getMember(senderId);

        // 4. STOMP 브로드캐스트 페이로드 구성
        StompMessagePayload payload = StompMessagePayload.builder()
                .messageId(msg.getMessageId())
                .roomId(roomId)
                .senderId(senderId)
                .senderNickname(sender.getNickname())
                .senderProfileImage(sender.getProfileImage())
                .content(content)
                .type(type)
                .isRead(false)
                .createdAt(msg.getCreatedAt())
                .build();

        // 5. 해당 방 구독자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat." + roomId, payload);

        log.debug("[Chat] 메시지 전송: room={}, sender={}, type={}", roomId, senderId, type);
        return payload;
    }

    // ── 미리보기 텍스트 ─────────────────────────────────────
    private String buildPreview(String content, MessageType type) {
        return switch (type) {
            case IMAGE       -> "[이미지]";
            case PRICE_OFFER -> "[가격 제안: " + content + "원]";
            default          -> content.length() > 100 ? content.substring(0, 100) + "..." : content;
        };
    }
}
