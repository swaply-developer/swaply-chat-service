package com.ch.swaplychatservice.config;

import com.ch.swaplychatservice.websocket.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket 엔드포인트
        // 프론트: brokerURL = 'ws://localhost:8882/ws'
        // HandshakeInterceptor는 브라우저 WebSocket API 제약으로 Authorization 헤더를
        // HTTP 업그레이드 요청에 실을 수 없으므로 사용하지 않는다.
        // JWT 검증은 configureClientInboundChannel 의 JwtChannelInterceptor 에서 처리.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 프레임에서 JWT 검증 후 memberId를 세션에 저장
        registration.interceptors(jwtChannelInterceptor);
    }
}
