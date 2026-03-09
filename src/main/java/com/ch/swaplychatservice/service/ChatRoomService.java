package com.ch.swaplychatservice.service;

import com.ch.swaplychatservice.dto.request.CreateRoomRequest;
import com.ch.swaplychatservice.dto.response.ChatRoomResponse;
import com.ch.swaplychatservice.dto.response.MemberInfo;
import com.ch.swaplychatservice.dto.response.ProductInfo;
import com.ch.swaplychatservice.entity.ChatRoom;
import com.ch.swaplychatservice.repository.ChatMessageRepository;
import com.ch.swaplychatservice.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository    roomRepository;
    private final ChatMessageRepository messageRepository;
    private final MemberQueryService    memberQuery;

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
                    log.info("채팅방 생성: roomId={}, productId={}, buyer={}, seller={}",
                            newRoom.getRoomId(), productId, buyerId, sellerId);
                    return buildResponse(newRoom, buyerId);
                });
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
