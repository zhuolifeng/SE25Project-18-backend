package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private Long paperId;
    private String paperTitle;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String type;
    private String category;
    private int status;
    private Set<String> tags;
} 