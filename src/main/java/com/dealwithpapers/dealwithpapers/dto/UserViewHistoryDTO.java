package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserViewHistoryDTO {
    private Long id;
    private Long userId;
    private Long paperId;
    private String paperTitle; // 论文标题，方便前端显示
    private LocalDateTime viewTime;
} 