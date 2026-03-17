package com.ch.swaplychatservice.service;

import com.ch.swaplychatservice.dto.message.ChatNotificationMessage;
import com.ch.swaplychatservice.dto.request.CreateRoomRequest;
import com.ch.swaplychatservice.dto.response.ChatRoomResponse;
import com.ch.swaplychatservice.dto.response.MemberInfo;
import com.ch.swaplychatservice.dto.response.ProductInfo;
import com.ch.swaplychatservice.entity.ChatRoom;
import com.ch.swaplychatservice.repository.ChatMessageRepository;
import com.ch.swaplychatservice.repository.ChatRoomRepository;
import com.ch.swaplychatservice.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository    roomRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageService       chatMessageService;
    private final MemberQueryService    memberQuery;
    private final RabbitTemplate rabbitTemplate;

    // ── 내 채팅방 목록 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getMyRooms(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> rooms = roomRepository.findByParticipant(memberId, pageable);

        return rooms.map(room -> buildResponse(room, memberId));
    }

    // ── 단일 채팅방 조회 + 읽음 처리 ───────────────────────
    @Transactional
    public ChatRoomResponse getRoom(Long roomId, Long memberId) {
        ChatRoom room = findRoomAndCheckAccess(roomId, memberId);

        // 읽음 처리
        room.markRead(memberId);
        messageRepository.markAllRead(roomId, memberId);

        return buildResponse(room, memberId);
    }

    // ── 채팅방 생성 (없으면 생성, 있으면 기존 방 반환) ─────
    @Transactional
    public ChatRoomResponse createRoom(CreateRoomRequest request, Long buyerId) {
        Long productId = request.getProductId();
        Long sellerId  = request.getSellerId();

        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("본인 상품에는 채팅할 수 없습니다");
        }

        // 기존 방 확인
        return roomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .map(existingRoom -> {
                    log.info("기존 채팅방 반환: roomId={}", existingRoom.getRoomId());
                    return buildResponse(existingRoom, buyerId);
                })
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.create(productId, buyerId, sellerId);
                    roomRepository.save(newRoom);

                    try {
                        // 알림에 필요한 부가 정보(닉네임, 상품명 등) 조회
                        ProductInfo product = memberQuery.getProduct(productId);
                        MemberInfo buyer = memberQuery.getMember(buyerId);

                        // ✅ 제공해주신 양식(ChatNotificationMessage)에 맞춰 데이터 구성
                        ChatNotificationMessage message = ChatNotificationMessage.builder()
                                .roomId(newRoom.getRoomId())
                                .senderId(buyerId)
                                .senderNickname(buyer.getNickname())
                                .receiverId(sellerId)
                                .productTitle(product.getTitle())
                                .productThumbnailUrl(product.getThumbnailUrl())
                                .build();

                        // 관리자 페이지에서 본 'chat.notification.exchange'로 발송
                        // Routing Key는 보통 큐 이름에서 .queue를 뺀 값을 많이 씁니다. (Bindings 확인 필요)
                        rabbitTemplate.convertAndSend("chat.notification.exchange", "chat.notification", message);

                        log.info("[MQ-Chat] 메시지 발행 완료: roomId={}", newRoom.getRoomId());
                    } catch (Exception e) {
                        log.error("[MQ-Chat] 메시지 발행 실패: {}", e.getMessage());
                    }

                    return buildResponse(newRoom, buyerId);
                });
    }

    // ── 가격 제안 수락 ─────────────────────────────────────
    @Transactional
    public ChatRoomResponse acceptNegotiatedPrice(Long roomId, Long memberId, Long price) {
        ChatRoom room = findRoomAndCheckAccess(roomId, memberId);
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("유효하지 않은 협의 가격입니다.");
        }
        room.acceptNegotiatedPrice(price);
        // 수락 알림 발행
        chatMessageService.publishPriceAccepted(roomId, memberId, price);
        return buildResponse(room, memberId);
    }

    // ── 가격 제안 거절 ─────────────────────────────────────
    @Transactional
    public void rejectNegotiatedPrice(Long roomId, Long memberId, Long price) {
        ChatRoom room = findRoomAndCheckAccess(roomId, memberId);
        // 거절 알림 발행
        chatMessageService.publishPriceRejected(roomId, memberId, price);
    }

    // ── 협의 가격 초기화 (취소/반품 완료 시 payment-service 내부 통신) ──
    @Transactional
    public void resetNegotiatedPrice(Long productId, Long buyerId) {
        roomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .ifPresent(room -> room.acceptNegotiatedPrice(null));
    }

    // ── 협의 가격 조회 (payment-service 내부 통신용) ───────────
    @Transactional(readOnly = true)
    public Long getNegotiatedPrice(Long productId, Long buyerId) {
        return roomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .map(ChatRoom::getNegotiatedPrice)
                .orElse(null);
    }

    // ── 접근 권한 확인 ──────────────────────────────────────
    public ChatRoom findRoomAndCheckAccess(Long roomId, Long memberId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다: " + roomId));
        if (!room.isParticipant(memberId)) {
            throw new SecurityException("채팅방 접근 권한이 없습니다");
        }
        return room;
    }

    // ── 내부 빌더 ───────────────────────────────────────────
    private ChatRoomResponse buildResponse(ChatRoom room, Long viewerId) {
        ProductInfo product = memberQuery.getProduct(room.getProductId());
        MemberInfo  buyer   = memberQuery.getMember(room.getBuyerId());
        MemberInfo  seller  = memberQuery.getMember(room.getSellerId());

        return ChatRoomResponse.of(
                room, viewerId,
                product.getTitle(),
                product.getPrice(),
                product.getThumbnailUrl(),
                buyer.getNickname(),
                buyer.getProfileImage(),
                seller.getNickname(),
                seller.getProfileImage()
        );
    }
}
