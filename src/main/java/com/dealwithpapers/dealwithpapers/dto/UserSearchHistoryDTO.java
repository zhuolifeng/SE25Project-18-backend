package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchHistoryDTO {
    private Long id;
    private Long userId;
    private String searchText;
    private LocalDateTime searchTime;
} 