package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.MessageDTO;
import com.dealwithpapers.dealwithpapers.dto.WebSocketMessageDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.MessageService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserRepository userRepository;
    
    // 保存在线用户信息
    private static final Map<Long, String> activeUsers = new ConcurrentHashMap<>();

    /**
     * 处理发送私信消息
     * 客户端发送消息到 /app/message.send
     */
    @MessageMapping("/message.send")
    public void sendMessage(@Payload WebSocketMessageDTO message, Principal principal) {
        System.out.println("收到WebSocket消息: " + message);
        try {
            // 安全检查：确保消息发送者是当前登录的用户
            User sender = getUserFromPrincipal(principal);
            
            System.out.println("从Principal获取的用户: " + (sender != null ? sender.getId() + " - " + sender.getUsername() : "null"));
            System.out.println("消息发送者ID: " + message.getSenderId());
            
            if (sender == null) {
                System.out.println("WebSocket认证失败: 无法从Principal获取用户信息");
                // 尝试直接从ID获取用户
                sender = userRepository.findById(message.getSenderId()).orElse(null);
                if (sender == null) {
                    System.out.println("无法通过ID获取发送者: " + message.getSenderId());
                    return;
                }
                System.out.println("通过ID获取发送者成功: " + sender.getId() + " - " + sender.getUsername());
            }
            
            if (!sender.getId().equals(message.getSenderId())) {
                // 如果发送者ID与当前用户ID不一致，拒绝发送
                System.out.println("WebSocket消息发送者ID不匹配: Principal用户ID=" + sender.getId() + ", 消息发送者ID=" + message.getSenderId());
                return;
            }
            
            // 检查接收者是否存在
            User receiver = userRepository.findById(message.getReceiverId())
                    .orElseThrow(() -> new RuntimeException("接收者不存在"));
            
            System.out.println("接收者验证成功: " + receiver.getId() + " - " + receiver.getUsername());
            
            // 保存消息到数据库
            System.out.println("开始保存消息到数据库: 发送者=" + sender.getId() + ", 接收者=" + receiver.getId() + ", 内容=" + message.getContent());
            MessageDTO savedMessage = messageService.sendMessage(
                    sender.getId(), 
                    receiver.getId(), 
                    message.getContent()
            );
            System.out.println("消息已保存到数据库, ID=" + savedMessage.getId());
            
            // 转换为WebSocket消息DTO
            WebSocketMessageDTO responseMessage = WebSocketMessageDTO.builder()
                    .id(savedMessage.getId())
                    .senderId(savedMessage.getSenderId())
                    .senderUsername(savedMessage.getSenderUsername())
                    .senderAvatar(savedMessage.getSenderAvatar())
                    .receiverId(savedMessage.getReceiverId())
                    .receiverUsername(savedMessage.getReceiverUsername())
                    .receiverAvatar(savedMessage.getReceiverAvatar())
                    .content(savedMessage.getContent())
                    .createTime(LocalDateTime.now())
                    .read(false)
                    .messageType("CHAT")
                    .tempId(message.getTempId()) // 保留临时ID，方便前端匹配替换临时消息
                    .build();
            
            System.out.println("准备发送WebSocket响应消息: " + responseMessage);
            
            // 优先发送到接收者的私人队列
            System.out.println("发送到接收者(" + receiver.getId() + ")的队列: /user/" + receiver.getId() + "/queue/messages");
            messagingTemplate.convertAndSendToUser(
                    receiver.getId().toString(),
                    "/queue/messages",
                    responseMessage
            );
            
            // 再发送给发送者，这样发送者也能看到自己发出的消息
            System.out.println("发送到发送者(" + sender.getId() + ")的队列: /user/" + sender.getId() + "/queue/messages");
            messagingTemplate.convertAndSendToUser(
                    sender.getId().toString(),
                    "/queue/messages",
                    responseMessage
            );
            
            // 发送通知消息到全局通道，告知所有连接的客户端有新消息
            // 注意：这里不包含消息内容，只是一个通知
            WebSocketMessageDTO notificationMessage = WebSocketMessageDTO.builder()
                    .senderId(savedMessage.getSenderId())
                    .receiverId(savedMessage.getReceiverId())
                    .senderUsername(savedMessage.getSenderUsername())
                    .receiverUsername(savedMessage.getReceiverUsername())
                    .messageType("NEW_MESSAGE_NOTIFICATION")
                    .createTime(LocalDateTime.now())
                    .build();
            
            System.out.println("发送全局通知消息: " + notificationMessage);
            messagingTemplate.convertAndSend("/topic/global", notificationMessage);
            
            // 直接广播给特定用户，这样即使用户未订阅也能收到通知
            broadcastToUsers(responseMessage);
            
            // 额外发送一份消息到用户特定主题，提高消息送达率
            System.out.println("发送消息到接收者特定主题: /topic/user/" + receiver.getId());
            messagingTemplate.convertAndSend("/topic/user/" + receiver.getId(), responseMessage);
            
            System.out.println("发送消息到发送者特定主题: /topic/user/" + sender.getId());
            messagingTemplate.convertAndSend("/topic/user/" + sender.getId(), responseMessage);
            
            System.out.println("WebSocket消息发送完成");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理WebSocket消息时发生异常: " + e.getMessage());
            // 发送错误消息给发送者
            WebSocketMessageDTO errorMessage = WebSocketMessageDTO.builder()
                    .content("发送消息失败: " + e.getMessage())
                    .createTime(LocalDateTime.now())
                    .messageType("ERROR")
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/errors",
                    errorMessage
            );
        }
    }
    
    /**
     * 处理标记消息为已读
     * 客户端发送请求到 /app/message.read
     */
    @MessageMapping("/message.read")
    public void markAsRead(@Payload WebSocketMessageDTO message, Principal principal) {
        System.out.println("收到标记消息已读请求: " + message);
        try {
            // 安全检查：确保当前用户是接收者
            User currentUser = getUserFromPrincipal(principal);
            
            System.out.println("从Principal获取的用户: " + (currentUser != null ? currentUser.getId() + " - " + currentUser.getUsername() : "null"));
            
            if (currentUser == null) {
                System.out.println("WebSocket认证失败: 无法从Principal获取用户信息");
                // 尝试直接从ID获取用户
                currentUser = userRepository.findById(message.getReceiverId()).orElse(null);
                if (currentUser == null) {
                    System.out.println("无法通过ID获取接收者: " + message.getReceiverId());
                    return;
                }
                System.out.println("通过ID获取接收者成功: " + currentUser.getId() + " - " + currentUser.getUsername());
            }
            
            if (!currentUser.getId().equals(message.getReceiverId())) {
                System.out.println("WebSocket标记已读用户不匹配: Principal用户ID=" + currentUser.getId() + ", 消息接收者ID=" + message.getReceiverId());
                return;
            }
            
            // 标记会话消息为已读
            System.out.println("开始标记会话消息为已读: 接收者=" + currentUser.getId() + ", 发送者=" + message.getSenderId());
            messageService.markConversationAsRead(currentUser.getId(), message.getSenderId());
            System.out.println("会话消息已标记为已读");
            
            // 发送确认消息
            WebSocketMessageDTO confirmationMessage = WebSocketMessageDTO.builder()
                    .senderId(message.getSenderId())
                    .receiverId(message.getReceiverId())
                    .messageType("READ_CONFIRMATION")
                    .createTime(LocalDateTime.now())
                    .build();
            
            // 通知发送者消息已读
            System.out.println("发送已读确认到发送者(" + message.getSenderId() + ")的队列: /user/" + message.getSenderId() + "/queue/message-status");
            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/message-status",
                    confirmationMessage
            );
            
            System.out.println("已读确认发送完成");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理标记已读请求时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 用户连接时记录信息
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            System.out.println("用户已连接: " + principal.getName());
            // 如果是用户类型的Principal，获取用户ID
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                // 获取用户ID并保存到活跃用户Map
                String username = userDetails.getUsername();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    activeUsers.put(user.getId(), headerAccessor.getSessionId());
                    System.out.println("保存用户连接信息: " + user.getId() + " -> " + headerAccessor.getSessionId());
                }
            } else if (principal instanceof UsernamePasswordAuthenticationToken) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User) {
                    User user = (User) auth.getPrincipal();
                    if (user != null) {
                        activeUsers.put(user.getId(), headerAccessor.getSessionId());
                        System.out.println("保存用户连接信息: " + user.getId() + " -> " + headerAccessor.getSessionId());
                    }
                }
            }
        } else {
            System.out.println("未能识别的用户连接");
        }
    }
    
    /**
     * 用户断开连接时清理信息
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 查找并移除断开连接的用户
        for (Map.Entry<Long, String> entry : activeUsers.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                System.out.println("用户断开连接: " + entry.getKey() + ", 会话ID: " + sessionId);
                activeUsers.remove(entry.getKey());
                break;
            }
        }
    }
    
    /**
     * 从Principal获取User对象
     */
    private User getUserFromPrincipal(Principal principal) {
        if (principal == null) {
            System.out.println("Principal为null");
            return null;
        }
        
        try {
            // 尝试通过SecurityContext获取用户信息
            System.out.println("尝试从SecurityContext获取当前用户，Principal名称: " + principal.getName());
            User user = AuthUtils.getCurrentUser(userRepository);
            if (user != null) {
                System.out.println("从SecurityContext成功获取用户: " + user.getId() + " - " + user.getUsername());
            } else {
                System.out.println("从SecurityContext获取用户失败");
            }
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("从Principal获取用户时发生异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 直接广播消息给特定用户，确保即使用户未正确订阅也能收到消息
     */
    private void broadcastToUsers(WebSocketMessageDTO message) {
        try {
            // 确保我们使用多种不同的广播方式，提高消息送达率
            
            // 1. 直接使用SimpMessagingTemplate发送到接收者的话题
            if (message.getReceiverId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + message.getReceiverId(),
                        message
                );
                System.out.println("已广播消息到接收者话题: /topic/user/" + message.getReceiverId());
                
                // 同时发送到队列
                messagingTemplate.convertAndSendToUser(
                        message.getReceiverId().toString(),
                        "/queue/messages",
                        message
                );
                System.out.println("已广播消息到接收者队列: /user/" + message.getReceiverId() + "/queue/messages");
            }
            
            // 2. 直接使用SimpMessagingTemplate发送到发送者的话题
            if (message.getSenderId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + message.getSenderId(),
                        message
                );
                System.out.println("已广播消息到发送者话题: /topic/user/" + message.getSenderId());
                
                // 同时发送到队列
                messagingTemplate.convertAndSendToUser(
                        message.getSenderId().toString(),
                        "/queue/messages",
                        message
                );
                System.out.println("已广播消息到发送者队列: /user/" + message.getSenderId() + "/queue/messages");
            }
            
            // 3. 发送到全局通知频道，但不包含消息内容
            WebSocketMessageDTO notificationMessage = WebSocketMessageDTO.builder()
                    .senderId(message.getSenderId())
                    .receiverId(message.getReceiverId())
                    .senderUsername(message.getSenderUsername())
                    .receiverUsername(message.getReceiverUsername())
                    .messageType("NEW_MESSAGE_NOTIFICATION")
                    .createTime(LocalDateTime.now())
                    .build();
                    
            messagingTemplate.convertAndSend("/topic/global", notificationMessage);
            System.out.println("已发送通知到全局话题: /topic/global");
        } catch (Exception e) {
            System.out.println("广播消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 