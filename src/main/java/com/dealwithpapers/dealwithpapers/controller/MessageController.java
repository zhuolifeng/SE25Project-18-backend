package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.ConversationDTO;
import com.dealwithpapers.dealwithpapers.dto.MessageDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.MessageService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    private final UserRepository userRepository;
    
    /**
     * 获取当前用户
     * @return 当前用户
     */
    private User getCurrentUser() {
        return AuthUtils.getCurrentUser(userRepository);
    }
    
    /**
     * 发送消息
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送结果
     */
    @PostMapping("/send/{receiverId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long receiverId, @RequestBody Map<String, String> payload) {
        try {
            User currentUser = getCurrentUser();
            String content = payload.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "消息内容不能为空"
                ));
            }
            
            MessageDTO sentMessage = messageService.sendMessage(currentUser.getId(), receiverId, content);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "发送成功",
                "data", sentMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取与特定用户的会话
     * @param userId 对话用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 会话消息列表
     */
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            
            // 标记消息为已读
            messageService.markConversationAsRead(currentUser.getId(), userId);
            
            // 获取会话消息
            Page<MessageDTO> messages = messageService.getConversation(
                    currentUser.getId(), 
                    userId,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"))
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", messages
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取会话列表
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversationList() {
        try {
            User currentUser = getCurrentUser();
            List<ConversationDTO> conversations = messageService.getConversationList(currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", conversations
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 标记会话为已读
     * @param userId 对话用户ID
     * @return 操作结果
     */
    @PostMapping("/read/{userId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            messageService.markConversationAsRead(currentUser.getId(), userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "标记已读成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取未读消息数
     * @return 未读消息数
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadCount() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> unreadCount = messageService.getUnreadMessageCount(currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(unreadCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 删除消息
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        try {
            User currentUser = getCurrentUser();
            boolean deleted = messageService.deleteMessage(currentUser.getId(), messageId);
            
            return ResponseEntity.ok(Map.of(
                "success", deleted,
                "message", deleted ? "删除成功" : "删除失败"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 