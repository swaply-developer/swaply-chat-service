package com.ch.swaplychatservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * product-service 에서 가져온 상품 기본 정보 캐시용
 * Redis 에 "product:info:{productId}" 키로 저장
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {
    private Long   productId;
    private String title;
    private Long   price;
    private String thumbnailUrl;
    private Long   sellerId;
}