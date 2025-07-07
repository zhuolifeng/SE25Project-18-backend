package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {
    
    // 查询两个用户之间的消息
    @Query("SELECT m FROM UserMessage m WHERE " +
           "(m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1) " +
           "ORDER BY m.createTime DESC")
    Page<UserMessage> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);
    
    // 查询用户的所有未读消息数
    long countByReceiverIdAndReadFalse(Long receiverId);
    
    // 查询用户与特定用户之间的未读消息数
    @Query("SELECT COUNT(m) FROM UserMessage m WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId AND m.read = false")
    long countUnreadMessages(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
    
    // 查询用户收到的所有消息
    Page<UserMessage> findByReceiverIdOrderByCreateTimeDesc(Long receiverId, Pageable pageable);
    
    // 查询用户发送的所有消息
    Page<UserMessage> findBySenderIdOrderByCreateTimeDesc(Long senderId, Pageable pageable);
    
    // 获取用户的会话列表（涉及用户的唯一对话者）- 修改为返回ID而不是实体对象
    @Query("SELECT DISTINCT " + 
           "CASE WHEN m.sender.id = :userId THEN m.receiver.id ELSE m.sender.id END " + 
           "FROM UserMessage m " + 
           "WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    List<Long> findDistinctConversationUserIds(@Param("userId") Long userId);
    
    // 查找用户与另一用户的最新消息
    @Query("SELECT m FROM UserMessage m WHERE " + 
           "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " + 
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) " + 
           "ORDER BY m.createTime DESC")
    Page<UserMessage> findLatestMessage(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);
    
    // 标记特定对话中的所有消息为已读
    @Query("UPDATE UserMessage m SET m.read = true WHERE m.receiver.id = :userId AND m.sender.id = :otherUserId AND m.read = false")
    void markConversationAsRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
} 