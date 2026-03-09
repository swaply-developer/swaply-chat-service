package com.ch.swaplychatservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoomRequest {

    @NotNull(message = "productId 는 필수입니다")
    private Long productId;

    /** 판매자 ID — 프론트에서 상품 상세 페이지에서 알고 있는 정보 */
    @NotNull(message = "sellerId 는 필수입니다")
    private Long sellerId;
}
