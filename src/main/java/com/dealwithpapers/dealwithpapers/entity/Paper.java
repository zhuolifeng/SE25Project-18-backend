package com.dealwithpapers.dealwithpapers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "papers")
public class Paper {
    
    @Id
    private String id; // 论文ID，由外部提供，不自增
    
    @Column(nullable = false)
    private String title; // 论文标题
    
    @ElementCollection
    @CollectionTable(name = "paper_authors", joinColumns = @JoinColumn(name = "paper_id"))
    @Column(name = "author")
    private Set<String> authors = new HashSet<>(); // 论文作者，可能有多个
    
    @Column(columnDefinition = "TEXT")
    private String abstractText; // 论文摘要
    
    private Integer year; // 发布年份
    
    private String journal; // 期刊名称
    
    private String category; // 论文类别
    
    private String url; // 论文链接
} 