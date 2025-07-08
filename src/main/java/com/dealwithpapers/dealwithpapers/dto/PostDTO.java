package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

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
    
    // 添加关联论文ID列表
    private Set<Long> relatedPaperIds = new HashSet<>();
    
    // 添加关联论文对象列表，用于返回详细信息
    private List<PaperDTO> relatedPapers = new ArrayList<>();
    
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
    
    // 新字段的getter和setter
    public Set<Long> getRelatedPaperIds() {
        return relatedPaperIds;
    }
    
    public void setRelatedPaperIds(Set<Long> relatedPaperIds) {
        this.relatedPaperIds = relatedPaperIds;
    }
    
    public List<PaperDTO> getRelatedPapers() {
        return relatedPapers;
    }
    
    public void setRelatedPapers(List<PaperDTO> relatedPapers) {
        this.relatedPapers = relatedPapers;
    }
} 