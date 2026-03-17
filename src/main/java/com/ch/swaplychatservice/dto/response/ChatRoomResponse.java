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

    private String partnerNickname;
    private String partnerProfileImage;

    private String    lastMessage;
    private int       unreadCount;
    private RoomStatus status;

    /** 협의된 가격. null이면 원가 결제, non-null이면 이 가격으로 결제 */
    private Long negotiatedPrice;

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
                .negotiatedPrice(room.getNegotiatedPrice())  // ← 협의 가격 포함
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
