package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    private Long id;
    private String content;
    private Long postId;
    private Long userId;
    private String userName;
    private Long parentId;
    private LocalDateTime createTime;
    private List<CommentDTO> children;
} 