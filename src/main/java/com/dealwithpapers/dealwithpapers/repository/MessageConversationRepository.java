package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.MessageConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageConversationRepository extends JpaRepository<MessageConversation, Long> {
    
    // 查找两个用户之间的会话
    @Query("SELECT c FROM MessageConversation c WHERE " +
           "(c.user1.id = :userId1 AND c.user2.id = :userId2) OR " +
           "(c.user1.id = :userId2 AND c.user2.id = :userId1)")
    Optional<MessageConversation> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    // 获取用户的所有会话列表（用户1）
    @Query("SELECT c FROM MessageConversation c WHERE c.user1.id = :userId ORDER BY c.lastMessageTime DESC")
    Page<MessageConversation> findConversationsForUser1(@Param("userId") Long userId, Pageable pageable);
    
    // 获取用户的所有会话列表（用户2）
    @Query("SELECT c FROM MessageConversation c WHERE c.user2.id = :userId ORDER BY c.lastMessageTime DESC")
    Page<MessageConversation> findConversationsForUser2(@Param("userId") Long userId, Pageable pageable);
    
    // 获取用户的所有会话列表（两种情况合并）
    @Query("SELECT c FROM MessageConversation c WHERE c.user1.id = :userId OR c.user2.id = :userId ORDER BY c.lastMessageTime DESC")
    Page<MessageConversation> findAllConversationsForUser(@Param("userId") Long userId, Pageable pageable);
    
    // 重置未读消息计数
    @Query("UPDATE MessageConversation c SET " +
           "c.unreadCountUser1 = CASE WHEN c.user1.id = :userId THEN 0 ELSE c.unreadCountUser1 END, " +
           "c.unreadCountUser2 = CASE WHEN c.user2.id = :userId THEN 0 ELSE c.unreadCountUser2 END " +
           "WHERE (c.user1.id = :userId AND c.user2.id = :otherUserId) OR " +
           "(c.user2.id = :userId AND c.user1.id = :otherUserId)")
    void resetUnreadCount(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
} 