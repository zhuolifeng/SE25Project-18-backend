package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderAvatar;
    private Long receiverId;
    private String receiverUsername;
    private String receiverAvatar;
    private String content;
    private LocalDateTime createTime;
    private boolean read;
    private String messageType; // 消息类型，如 "CHAT", "NOTIFICATION", 等
    private String tempId; // 临时ID，用于前端匹配替换临时消息
} 