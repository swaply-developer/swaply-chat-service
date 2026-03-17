package com.ch.swaplychatservice.controller;

import com.ch.swaplychatservice.dto.request.AcceptPriceRequest;
import com.ch.swaplychatservice.dto.request.CreateRoomRequest;
import com.ch.swaplychatservice.dto.response.ChatMessageResponse;
import com.ch.swaplychatservice.dto.response.ChatRoomResponse;
import com.ch.swaplychatservice.service.ChatMessageService;
import com.ch.swaplychatservice.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService    chatRoomService;
    private final ChatMessageService chatMessageService;

    // ── 내 채팅방 목록
    // GET /api/chat/rooms?page=0&size=20
    @GetMapping("/rooms")
    public ResponseEntity<Page<ChatRoomResponse>> getMyRooms(
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info(">>>> 들어온 X-Member-Id 헤더: {}", memberIdHeader); // 이 로그가 null이면 Gateway 문제
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatRoomService.getMyRooms(memberId, page, size));
    }

    // ── 단일 채팅방 조회 (읽음 처리 포함)
    // GET /api/chat/rooms/{roomId}
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponse> getRoom(
            @PathVariable Long roomId,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader
    ) {
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatRoomService.getRoom(roomId, memberId));
    }

    // ── 채팅방 생성 (없으면 생성, 있으면 기존 반환)
    // POST /api/chat/rooms  { productId, sellerId }
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader
    ) {
        log.info(">>>> 들어온 헤더값: {}", memberIdHeader); // 👈 이게 null이면 Gateway 필터 문제
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        ChatRoomResponse response = chatRoomService.createRoom(request, memberId);
        return ResponseEntity.ok(response);
    }

    // ── 메시지 목록 (페이징, 오래된 순)
    // GET /api/chat/rooms/{roomId}/messages?page=0&size=50
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatMessageService.getMessages(roomId, memberId, page, size));
    }

    /**
     * PATCH /api/chat/rooms/{roomId}/accept-price
     * 가격 제안 수락 — 협의 가격을 chat_room에 저장
     */
    @PatchMapping("/rooms/{roomId}/accept-price")
    public ResponseEntity<ChatRoomResponse> acceptPrice(
            @PathVariable Long roomId,
            @RequestBody @Valid AcceptPriceRequest request,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader
    ) {
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(chatRoomService.acceptNegotiatedPrice(roomId, memberId, request.getPrice()));
    }

    /**
     * POST /api/chat/rooms/{roomId}/reject-price
     * 가격 제안 거절 — 알림 발행용
     */
    @PostMapping("/rooms/{roomId}/reject-price")
    public ResponseEntity<Void> rejectPrice(
            @PathVariable Long roomId,
            @RequestBody @Valid AcceptPriceRequest request,
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdHeader
    ) {
        Long memberId = parseMemberId(memberIdHeader);
        if (memberId == null) return ResponseEntity.status(401).build();

        chatRoomService.rejectNegotiatedPrice(roomId, memberId, request.getPrice());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/chat/internal/negotiated-price?productId=2&buyerId=3
     * payment-service 내부 통신 전용 — 협의 가격 조회
     */
    @GetMapping("/internal/negotiated-price")
    public ResponseEntity<Map<String, Long>> getNegotiatedPrice(
            @RequestParam Long productId,
            @RequestParam Long buyerId
    ) {
        Long price = chatRoomService.getNegotiatedPrice(productId, buyerId);
        return ResponseEntity.ok(Map.of("negotiatedPrice", price != null ? price : -1L));
    }

    // ── 헬퍼
    // parseMemberId 메서드 부분을 아래와 같이 수정 (또는 호출부 수정)
    private Long parseMemberId(String header) {
        if (header == null || header.isBlank() || header.equals("undefined")) {
            log.warn(">>>> [ChatService] X-Member-Id 헤더가 비어있습니다!");
            return null;
        }
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) { return null; }
    }
}
