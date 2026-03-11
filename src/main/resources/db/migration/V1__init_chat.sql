-- ============================================================
-- Swaply Chat Service — V1 DDL
-- product service 의 member, product 테이블을 FK 로 참조
-- ============================================================

-- 채팅방
CREATE TABLE IF NOT EXISTS chat_room (
    room_id      BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_id   BIGINT       NOT NULL,
    buyer_id     BIGINT       NOT NULL,
    seller_id    BIGINT       NOT NULL,
    last_message TEXT,
    buyer_unread INT          NOT NULL DEFAULT 0,
    seller_unread INT         NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CLOSED
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 같은 상품에 대해 구매자 하나당 방 하나
    UNIQUE KEY uq_product_buyer (product_id, buyer_id),

    CONSTRAINT fk_room_product  FOREIGN KEY (product_id) REFERENCES product(product_id),
    CONSTRAINT fk_room_buyer    FOREIGN KEY (buyer_id)   REFERENCES member(member_id),
    CONSTRAINT fk_room_seller   FOREIGN KEY (seller_id)  REFERENCES member(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅방';

-- 채팅 메시지
CREATE TABLE IF NOT EXISTS chat_message (
    message_id  BIGINT      PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT      NOT NULL,
    sender_id   BIGINT      NOT NULL,
    content     TEXT        NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'TEXT',   -- TEXT | IMAGE | PRICE_OFFER
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_room_created (room_id, created_at),

    CONSTRAINT fk_msg_room   FOREIGN KEY (room_id)   REFERENCES chat_room(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES member(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 메시지';
