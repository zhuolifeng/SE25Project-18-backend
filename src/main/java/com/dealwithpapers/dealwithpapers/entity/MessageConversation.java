package com.dealwithpapers.dealwithpapers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_conversations")
public class MessageConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    @OneToOne
    @JoinColumn(name = "last_message_id")
    private UserMessage lastMessage;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime = LocalDateTime.now();
    
    @Column(name = "unread_count_user1")
    private int unreadCountUser1 = 0;
    
    @Column(name = "unread_count_user2")
    private int unreadCountUser2 = 0;
} 