package com.ch.swaplychatservice.controller;

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

    // ── 헬퍼
    private Long parseMemberId(String header) {
        if (header == null || header.isBlank()) return null;
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) { return null; }
    }
}
