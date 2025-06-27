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
@Table(name = "user_search_history")
public class UserSearchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "search_text", columnDefinition = "TEXT", nullable = false)
    private String searchText;
    
    @Column(name = "search_time")
    private LocalDateTime searchTime;
} 