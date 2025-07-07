package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.ConversationDTO;
import com.dealwithpapers.dealwithpapers.dto.MessageDTO;
import com.dealwithpapers.dealwithpapers.entity.MessageConversation;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserMessage;
import com.dealwithpapers.dealwithpapers.repository.MessageConversationRepository;
import com.dealwithpapers.dealwithpapers.repository.UserMessageRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserMessageRepository userMessageRepository;
    private final MessageConversationRepository messageConversationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MessageDTO sendMessage(Long senderId, Long receiverId, String content) {
        // 查找发送者和接收者
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("发送者不存在"));
        
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("接收者不存在"));
        
        // 创建新消息
        UserMessage message = new UserMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setRead(false);
        message.setCreateTime(LocalDateTime.now());
        
        // 保存消息
        UserMessage savedMessage = userMessageRepository.save(message);
        
        // 更新或创建会话
        updateOrCreateConversation(senderId, receiverId, savedMessage);
        
        return convertToDTO(savedMessage);
    }

    @Override
    public Page<MessageDTO> getConversation(Long userId, Long otherUserId, Pageable pageable) {
        // 验证用户存在
        if (!userRepository.existsById(userId) || !userRepository.existsById(otherUserId)) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取会话消息
        Page<UserMessage> messages = userMessageRepository.findConversation(userId, otherUserId, pageable);
        
        // 转换为DTO
        return messages.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public List<ConversationDTO> getConversationList(Long userId) {
        // 验证用户存在
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 获取所有与用户有对话的用户ID
        List<Long> conversationUserIds = userMessageRepository.findDistinctConversationUserIds(userId);
        
        List<ConversationDTO> conversations = new ArrayList<>();
        for (Long otherUserId : conversationUserIds) {
            // 获取对话用户信息
            User otherUser = userRepository.findById(otherUserId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 获取最新的一条消息
            Page<UserMessage> latestMessagePage = userMessageRepository.findLatestMessage(
                    userId, otherUserId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createTime")));
            
            if (latestMessagePage.hasContent()) {
                UserMessage latestMessage = latestMessagePage.getContent().get(0);
                
                // 获取未读消息数
                long unreadCount = userMessageRepository.countUnreadMessages(userId, otherUserId);
                
                ConversationDTO dto = new ConversationDTO();
                dto.setUserId(otherUser.getId());
                dto.setUsername(otherUser.getUsername());
                dto.setLastMessage(latestMessage.getContent());
                dto.setLastMessageTime(latestMessage.getCreateTime());
                dto.setUnreadCount((int) unreadCount);
                dto.setOnline(false); // 默认离线状态，实际应用中可以通过其他服务判断
                
                conversations.add(dto);
            }
        }
        
        // 按最后消息时间排序
        conversations.sort(Comparator.comparing(ConversationDTO::getLastMessageTime).reversed());
        
        return conversations;
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long userId, Long otherUserId) {
        // 验证用户存在
        if (!userRepository.existsById(userId) || !userRepository.existsById(otherUserId)) {
            throw new RuntimeException("用户不存在");
        }
        
        // 标记消息为已读
        // 由于直接查询更新效率较低，这里使用手动查询和批量更新
        Page<UserMessage> unreadMessages = userMessageRepository.findByReceiverIdOrderByCreateTimeDesc(
                userId, PageRequest.of(0, 1000));
        
        if (unreadMessages.hasContent()) {
            List<UserMessage> messagesToUpdate = unreadMessages.getContent().stream()
                    .filter(m -> m.getSender().getId().equals(otherUserId) && !m.isRead())
                    .collect(Collectors.toList());
            
            for (UserMessage message : messagesToUpdate) {
                message.setRead(true);
            }
            
            userMessageRepository.saveAll(messagesToUpdate);
        }
        
        // 更新会话的未读计数
        Optional<MessageConversation> conversation = messageConversationRepository.findConversation(userId, otherUserId);
        if (conversation.isPresent()) {
            MessageConversation conv = conversation.get();
            if (conv.getUser1().getId().equals(userId)) {
                conv.setUnreadCountUser1(0);
            } else {
                conv.setUnreadCountUser2(0);
            }
            messageConversationRepository.save(conv);
        }
    }

    @Override
    public Map<String, Object> getUnreadMessageCount(Long userId) {
        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取总未读消息数
        long totalUnread = userMessageRepository.countByReceiverIdAndReadFalse(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalUnread", totalUnread);
        
        return result;
    }

    @Override
    public MessageDTO getMessage(Long messageId) {
        // 获取消息
        UserMessage message = userMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        return convertToDTO(message);
    }

    @Override
    @Transactional
    public boolean deleteMessage(Long userId, Long messageId) {
        // 获取消息
        UserMessage message = userMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        // 检查权限（只能删除自己发送的消息）
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("没有权限删除此消息");
        }
        
        // 删除消息
        userMessageRepository.delete(message);
        
        return true;
    }
    
    // 辅助方法: 将实体转换为DTO
    private MessageDTO convertToDTO(UserMessage message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverUsername(message.getReceiver().getUsername());
        dto.setContent(message.getContent());
        dto.setRead(message.isRead());
        dto.setCreateTime(message.getCreateTime());
        return dto;
    }
    
    // 辅助方法: 更新或创建会话
    private void updateOrCreateConversation(Long senderId, Long receiverId, UserMessage message) {
        // 确保user1的ID始终小于user2的ID，保证会话唯一性
        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);
        
        // 查找是否已存在会话
        Optional<MessageConversation> conversationOptional = 
                messageConversationRepository.findConversation(user1Id, user2Id);
        
        MessageConversation conversation;
        if (conversationOptional.isPresent()) {
            conversation = conversationOptional.get();
            conversation.setLastMessage(message);
            conversation.setLastMessageTime(message.getCreateTime());
            
            // 更新未读计数
            if (message.getReceiver().getId().equals(user1Id)) {
                conversation.setUnreadCountUser1(conversation.getUnreadCountUser1() + 1);
            } else {
                conversation.setUnreadCountUser2(conversation.getUnreadCountUser2() + 1);
            }
        } else {
            // 创建新会话
            conversation = new MessageConversation();
            conversation.setUser1(userRepository.findById(user1Id).orElseThrow());
            conversation.setUser2(userRepository.findById(user2Id).orElseThrow());
            conversation.setLastMessage(message);
            conversation.setLastMessageTime(message.getCreateTime());
            
            // 设置初始未读计数
            if (message.getReceiver().getId().equals(user1Id)) {
                conversation.setUnreadCountUser1(1);
                conversation.setUnreadCountUser2(0);
            } else {
                conversation.setUnreadCountUser1(0);
                conversation.setUnreadCountUser2(1);
            }
        }
        
        messageConversationRepository.save(conversation);
    }
} 