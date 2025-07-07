package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.ConversationDTO;
import com.dealwithpapers.dealwithpapers.dto.MessageDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface MessageService {
    
    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送的消息
     */
    MessageDTO sendMessage(Long senderId, Long receiverId, String content);
    
    /**
     * 获取用户之间的会话消息
     * @param userId 当前用户ID
     * @param otherUserId 对话者ID
     * @param pageable 分页参数
     * @return 会话消息列表
     */
    Page<MessageDTO> getConversation(Long userId, Long otherUserId, Pageable pageable);
    
    /**
     * 获取用户所有会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ConversationDTO> getConversationList(Long userId);
    
    /**
     * 标记特定会话中的消息为已读
     * @param userId 当前用户ID
     * @param otherUserId 对话者ID
     */
    void markConversationAsRead(Long userId, Long otherUserId);
    
    /**
     * 获取用户未读消息数
     * @param userId 用户ID
     * @return 未读消息数
     */
    Map<String, Object> getUnreadMessageCount(Long userId);
    
    /**
     * 获取单条消息详情
     * @param messageId 消息ID
     * @return 消息详情
     */
    MessageDTO getMessage(Long messageId);
    
    /**
     * 删除单条消息
     * @param userId 当前用户ID（只能删除自己的消息）
     * @param messageId 消息ID
     * @return 操作结果
     */
    boolean deleteMessage(Long userId, Long messageId);
} 