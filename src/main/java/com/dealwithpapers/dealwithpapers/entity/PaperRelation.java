package com.dealwithpapers.dealwithpapers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paper_relations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_paper_id", "target_doi", "relation_type"}),
       indexes = {
           @Index(name = "idx_source_paper_type", columnList = "source_paper_id, relation_type"),
           @Index(name = "idx_target_paper_type", columnList = "target_paper_id, relation_type"),
           @Index(name = "idx_priority_score", columnList = "priority_score DESC")
       })
public class PaperRelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_paper_id", nullable = false)
    private Long sourcePaperId;
    
    @Column(name = "target_paper_id")
    private Long targetPaperId;
    
    @Column(name = "relation_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RelationType relationType;
    
    @Column(name = "target_title", nullable = false, length = 500)
    private String targetTitle;
    
    @Column(name = "target_doi", length = 200)
    private String targetDoi;
    
    @Column(name = "target_authors", length = 1000)
    private String targetAuthors;
    
    @Column(name = "target_year")
    private Integer targetYear;
    
    @Column(name = "citation_count")
    private Integer citationCount;
    
    @Column(name = "influential_citation_count")
    private Integer influentialCitationCount;
    
    @Column(name = "target_venue", length = 200)
    private String targetVenue;
    
    @Column(name = "target_abstract", length = 2000)
    private String targetAbstract;
    
    @Column(name = "semantic_scholar_id", length = 100)
    private String semanticScholarId;
    
    @Column(name = "citation_intent", length = 200)
    private String citationIntent; // background,methodology,result
    
    @Column(name = "open_access_url", length = 500)
    private String openAccessUrl;
    
    @Column(name = "priority_score")
    private Double priorityScore;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_paper_id", insertable = false, updatable = false)
    private Paper sourcePaper;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_paper_id", insertable = false, updatable = false)
    private Paper targetPaper;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
    
    public enum RelationType {
        REFERENCES,    // 原论文引用的论文
        CITED_BY       // 引用原论文的论文
    }
}