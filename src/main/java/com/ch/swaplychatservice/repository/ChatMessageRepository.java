package com.ch.swaplychatservice.repository;

import com.ch.swaplychatservice.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 특정 방의 메시지 페이징 (오래된 순) */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.room.roomId = :roomId
        ORDER BY m.createdAt ASC
    """)
    Page<ChatMessage> findByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    /** 방에서 안 읽은 메시지 개수 */
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.room.roomId = :roomId
          AND m.senderId != :memberId
          AND m.isRead = false
    """)
    long countUnread(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    /** 방 입장 시 일괄 읽음 처리 */
    @Modifying
    @Query("""
        UPDATE ChatMessage m SET m.isRead = true
        WHERE m.room.roomId = :roomId
          AND m.senderId != :memberId
          AND m.isRead = false
    """)
    int markAllRead(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
