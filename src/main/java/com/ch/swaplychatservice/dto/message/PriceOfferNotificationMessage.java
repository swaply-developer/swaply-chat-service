package com.ch.swaplychatservice.dto.message;

/**
 * chat-service → notification-service
 * 가격 제안 / 수락 / 거절 이벤트 발행
 *
 * Exchange : chat.price.offer.exchange (DirectExchange)
 * Queue    : chat.price.offer.queue
 * Key      : chat.price.offer
 *
 * eventType:
 *   OFFERED  — 가격 제안
 *   ACCEPTED — 제안 수락
 *   REJECTED — 제안 거절
 */
public record PriceOfferNotificationMessage(
        String eventType,          // OFFERED | ACCEPTED | REJECTED
        Long   roomId,
        Long   productId,
        String productTitle,
        String productThumbnailUrl,
        Long   actorId,            // 이벤트 발생자 (제안자 or 수락자 or 거절자)
        String actorNickname,
        Long   buyerId,
        Long   sellerId,
        Long   price               // 협의 금액
) {}
