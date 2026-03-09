package com.ch.swaplychatservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        log.info("[WS Handshake] 연결 시도 시작: {}", request.getURI());

        // 1. 헤더에서 Authorization 추출
        String authHeader = request.getHeaders().getFirst("Authorization");
        log.info("[WS Handshake] Authorization 헤더 값: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("[WS Handshake] 추출된 토큰 확인 완료");

            try {
                // TODO: 여기서 실제로 토큰 검증 로직이 있다면 로그를 남기세요.
                // 예: log.info("[WS Handshake] 토큰 사용자: {}", jwtProvider.getUserId(token));

                // 임시로 attributes에 담아두기 (나중에 WebSocketSession에서 꺼내 쓸 수 있음)
                attributes.put("token", token);
                log.info("[WS Handshake] 인증 성공 - 연결 허용");
                return true;
            } catch (Exception e) {
                log.error("[WS Handshake] 토큰 검증 중 에러 발생: {}", e.getMessage());
                return false;
            }
        }

        log.warn("[WS Handshake] 인증 헤더가 없거나 형식이 올바르지 않음 - 연결 거부");
        return false; // 여기서 false를 리턴하면 연결이 즉시 종료됩니다.
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        log.info("[WS Handshake] 핸드셰이크 완료");
    }
}