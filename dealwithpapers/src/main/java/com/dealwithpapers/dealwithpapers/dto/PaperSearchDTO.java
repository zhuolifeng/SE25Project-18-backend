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
} 