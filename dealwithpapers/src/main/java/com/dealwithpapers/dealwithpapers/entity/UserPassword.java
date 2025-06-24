package com.dealwithpapers.dealwithpapers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_passwords")
public class UserPassword {
    
    @Id
    private Long userId;
    
    @Column(nullable = false)
    private String password;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
} 