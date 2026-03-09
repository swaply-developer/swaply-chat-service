package com.ch.swaplychatservice.repository;

import com.ch.swaplychatservice.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /** 내가 구매자 또는 판매자인 방 목록 (최근 활동순) */
    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.buyerId = :memberId OR r.sellerId = :memberId
        ORDER BY r.updatedAt DESC
    """)
    Page<ChatRoom> findByParticipant(@Param("memberId") Long memberId, Pageable pageable);

    /** 상품 + 구매자로 기존 방 찾기 (중복 방지) */
    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
}
