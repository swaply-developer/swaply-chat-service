package com.ch.swaplychatservice.dto.response;

import com.ch.swaplychatservice.entity.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * STOMP 메시지 페이로드
 *   - 클라이언트 → 서버 : roomId + content + type 만 있으면 됨
 *   - 서버 → 클라이언트 : 전체 필드 채워서 브로드캐스트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StompMessagePayload {

    // ── 클라이언트 → 서버
    private Long        roomId;
    private String      content;
    private MessageType type;

    // ── 서버 → 클라이언트 (브로드캐스트 시 채워짐)
    private Long        messageId;
    private Long        senderId;
    private String      senderNickname;
    private String      senderProfileImage;
    private boolean     isRead;
    private LocalDateTime createdAt;
}
