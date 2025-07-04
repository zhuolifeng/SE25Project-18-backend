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
    private Set<String> postTags;
    
    // Explicit getters to ensure they're available
    public Long getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
    }
    
    public Long getAuthorId() {
        return authorId;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public Long getPaperId() {
        return paperId;
    }
    
    public String getPaperTitle() {
        return paperTitle;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public String getType() {
        return type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getStatus() {
        return status;
    }
    
    public Set<String> getPostTags() {
        return postTags;
    }
} 