package com.ch.swaplychatservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ── chat notification (기존) ──────────────────────────
    public static final String CHAT_NOTIFICATION_EXCHANGE = "chat.notification.exchange";
    public static final String CHAT_NOTIFICATION_KEY      = "chat.notification";

    // ── price offer notification (신규) ──────────────────
    public static final String PRICE_OFFER_EXCHANGE = "chat.price.offer.exchange";
    public static final String PRICE_OFFER_QUEUE    = "chat.price.offer.queue";
    public static final String PRICE_OFFER_KEY      = "chat.price.offer";

    @Bean
    public DirectExchange priceOfferExchange() {
        return new DirectExchange(PRICE_OFFER_EXCHANGE, true, false);
    }

    // 1. JSON 변환기 빈 등록
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 2. RabbitTemplate에 JSON 변환기 설정
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}