package com.ch.swaplychatservice.dto.message;

import lombok.Builder;

@Builder
public record ChatNotificationMessage(
        Long roomId,
        Long senderId,
        String senderNickname,
        Long receiverId,
        String productTitle,
        String productThumbnailUrl
) {}