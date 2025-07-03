package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPaperTagDTO {
    private Long id;
    private Long userId;
    private Long paperId;
    private String tagName;
    private String tagColor;
    private LocalDateTime createdAt;
    
    // 附加信息 - 用于前端展示
    private String paperTitle;
    private String paperAuthors;
    private String paperYear;
} 