package com.ch.swaplychatservice.config;

import com.ch.swaplychatservice.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 서버 → 클라이언트 브로드캐스트 prefix
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");

        // 특정 사용자 전송 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
//                .addInterceptors(jwtHandshakeInterceptor);
        // ✅ .withSockJS() 를 삭제했습니다.
        // MSA 게이트웨이 환경에서는 순수 WebSocket이 더 안정적입니다.
    }
}