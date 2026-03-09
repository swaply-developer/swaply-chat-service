package com.ch.swaplychatservice.entity;

public enum MessageType {
    TEXT,        // 일반 텍스트
    IMAGE,       // 이미지 (Base64 or URL)
    PRICE_OFFER  // 가격 제안
}