package com.ch.swaplychatservice.dto.response;

import com.ch.swaplychatservice.entity.ChatRoom;
import com.ch.swaplychatservice.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {

    private Long   roomId;
    private Long   productId;
    private String productTitle;
    private Long   productPrice;
    private String productThumbnail;

    private Long   buyerId;
    private String buyerNickname;
    private String buyerProfileImage;

    private Long   sellerId;
    private String sellerNickname;
    private String sellerProfileImage;

    // 조회 시점의 내 상대방 정보 (편의 필드)
    private String partnerNickname;
    private String partnerProfileImage;

    private String lastMessage;
    private int    unreadCount;   // 나 기준 안 읽은 수
    private RoomStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatRoomResponse of(ChatRoom room,
                                      Long viewerId,
                                      String productTitle,
                                      Long productPrice,
                                      String productThumbnail,
                                      String buyerNickname,
                                      String buyerProfileImage,
                                      String sellerNickname,
                                      String sellerProfileImage) {
        boolean isBuyer = viewerId.equals(room.getBuyerId());

        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .productId(room.getProductId())
                .productTitle(productTitle)
                .productPrice(productPrice)
                .productThumbnail(productThumbnail)
                .buyerId(room.getBuyerId())
                .buyerNickname(buyerNickname)
                .buyerProfileImage(buyerProfileImage)
                .sellerId(room.getSellerId())
                .sellerNickname(sellerNickname)
                .sellerProfileImage(sellerProfileImage)
                .partnerNickname(isBuyer ? sellerNickname : buyerNickname)
                .partnerProfileImage(isBuyer ? sellerProfileImage : buyerProfileImage)
                .lastMessage(room.getLastMessage())
                .unreadCount(isBuyer ? room.getBuyerUnread() : room.getSellerUnread())
                .status(room.getStatus())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
