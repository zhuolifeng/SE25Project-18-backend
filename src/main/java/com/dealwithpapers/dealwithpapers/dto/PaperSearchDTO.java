package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSearchDTO {
    private String searchTerm; // 单一搜索框，可用于ID、标题或作者
    private String conferenceName; // 新增：会议名
    private Integer year; // 新增：年份
    private String topic; // 新增：主题
    private String keyword; // 新增：关键词
    private String arxivCategory; // 新增：arxiv学科分类
    
    // Explicit getters
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public Integer getYear() {
        return year;
    }
    
    // Explicit setters
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
} 