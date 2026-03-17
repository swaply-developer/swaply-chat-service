package com.ch.swaplychatservice.service;

import com.ch.swaplychatservice.config.RabbitConfig;
import com.ch.swaplychatservice.dto.message.PriceOfferNotificationMessage;
import com.ch.swaplychatservice.dto.response.ChatMessageResponse;
import com.ch.swaplychatservice.dto.response.ProductInfo;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate           rabbitTemplate;

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

        // 6. PRICE_OFFER 타입이면 알림 발행
        if (type == MessageType.PRICE_OFFER) {
            publishPriceOfferNotification("OFFERED", room, senderId, sender, content);
        }

        log.debug("[Chat] 메시지 전송: room={}, sender={}, type={}", roomId, senderId, type);
        return payload;
    }

    /**
     * 가격 제안 수락 알림 발행 (ChatRoomService에서 호출)
     * acceptorId: 수락한 사람, price: 협의 금액
     */
    public void publishPriceAccepted(Long roomId, Long acceptorId, Long price) {
        try {
            ChatRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));
            MemberInfo acceptor = memberQuery.getMember(acceptorId);
            publishPriceOfferNotification("ACCEPTED", room, acceptorId, acceptor,
                    String.valueOf(price));
        } catch (Exception e) {
            log.warn("[Chat] 수락 알림 발행 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    /**
     * 가격 제안 거절 알림 발행 (ChatRoomService에서 호출)
     */
    public void publishPriceRejected(Long roomId, Long rejectorId, Long price) {
        try {
            ChatRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));
            MemberInfo rejector = memberQuery.getMember(rejectorId);
            publishPriceOfferNotification("REJECTED", room, rejectorId, rejector,
                    String.valueOf(price));
        } catch (Exception e) {
            log.warn("[Chat] 거절 알림 발행 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    private void publishPriceOfferNotification(String eventType, ChatRoom room,
                                                Long actorId, MemberInfo actor,
                                                String priceStr) {
        try {
            ProductInfo product = memberQuery.getProduct(room.getProductId());
            long price = Long.parseLong(priceStr.replaceAll("[^0-9]", ""));

            PriceOfferNotificationMessage msg = new PriceOfferNotificationMessage(
                    eventType,
                    room.getRoomId(),
                    room.getProductId(),
                    product.getTitle(),
                    product.getThumbnailUrl(),
                    actorId,
                    actor.getNickname(),
                    room.getBuyerId(),
                    room.getSellerId(),
                    price
            );
            rabbitTemplate.convertAndSend(
                    RabbitConfig.PRICE_OFFER_EXCHANGE,
                    RabbitConfig.PRICE_OFFER_KEY,
                    msg
            );
            log.info("[Chat] 가격 제안 알림 발행: event={}, roomId={}, price={}", eventType, room.getRoomId(), price);
        } catch (Exception e) {
            log.warn("[Chat] 가격 제안 알림 발행 실패: event={}, error={}", eventType, e.getMessage());
        }
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
