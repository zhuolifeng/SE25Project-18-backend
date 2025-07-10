package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private Long receiverId;
    private String receiverUsername;
    private String content;
    private boolean read;
    private LocalDateTime createTime;
    private String senderAvatar; // 发送者头像url
    private String receiverAvatar; // 接收者头像url
} 