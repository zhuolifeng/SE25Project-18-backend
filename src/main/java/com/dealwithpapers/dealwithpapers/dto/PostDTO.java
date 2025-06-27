package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private String paperId;
    private String paperTitle;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String type;
    private String category;
    private int status;
} 