package com.ch.swaplychatservice.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * STOMP CONNECT 프레임에서 JWT를 검증하고 memberId를 세션에 저장한다.
 *
 * ─ 왜 HandshakeInterceptor가 아닌가 ───────────────────────────────
 * 브라우저 WebSocket API는 HTTP 업그레이드 요청에 커스텀 헤더를 실을 수 없다.
 * @stomp/stompjs 의 connectHeaders 는 WebSocket 연결 '이후' 전송되는
 * STOMP CONNECT 프레임에 실리므로, 인증은 STOMP 레이어에서 해야 한다.
 * ─────────────────────────────────────────────────────────────────
 */
@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final SecretKey secretKey;

    public JwtChannelInterceptor(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임만 처리, 나머지는 그냥 통과
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        log.info("[STOMP] CONNECT 수신: session={}", accessor.getSessionId());

        // STOMP connectHeaders 에서 Authorization 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[STOMP] Authorization 헤더 없음 → 연결 거부");
            throw new AccessDeniedException("Authorization 헤더가 필요합니다");
        }

        String token = authHeader.substring(7).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long memberId = Long.parseLong(claims.getSubject());

            // WebSocket 세션 attributes 에 memberId 저장
            // → ChatWebSocketController 에서 sessionAttrs.get("memberId") 로 꺼냄
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            if (sessionAttrs != null) {
                sessionAttrs.put("memberId", memberId);
            }

            log.info("[STOMP] 인증 성공: memberId={}", memberId);

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[STOMP] JWT 검증 실패: {}", e.getMessage());
            throw new AccessDeniedException("유효하지 않은 토큰입니다");
        }

        return message;
    }
}
