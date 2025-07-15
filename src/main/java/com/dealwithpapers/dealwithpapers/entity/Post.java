package com.dealwithpapers.dealwithpapers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import com.dealwithpapers.dealwithpapers.entity.PostTag;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    // 原有的单个论文关联（作为主要论文）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    // 添加多对多关联，允许一个帖子关联多篇论文
    @ManyToMany
    @JoinTable(
        name = "post_related_papers",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "paper_id")
    )
    private Set<Paper> relatedPapers = new HashSet<>();

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(length = 50)
    private String type;

    @Column(length = 50)
    private String category;

    @Column(name = "status", nullable = false)
    private int status = 1;

    @Column(name = "views", nullable = false)
    private int views = 0;

    @ManyToMany
    @JoinTable(
        name = "post_relation_tag",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<PostTag> tags = new HashSet<>();
} 