-- V2: 채팅 가격 협의 기능 추가
-- 가격 제안 수락 시 협의된 가격을 chat_room에 저장
ALTER TABLE chat_room
    ADD COLUMN negotiated_price BIGINT NULL COMMENT '가격 제안 합의 금액 (null=원가 결제)';
