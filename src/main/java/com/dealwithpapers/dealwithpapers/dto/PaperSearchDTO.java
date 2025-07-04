package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSearchDTO {
    private String searchTerm; // 单一搜索框，可用于ID、标题或作者
    private Integer year; // 年份筛选
    
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