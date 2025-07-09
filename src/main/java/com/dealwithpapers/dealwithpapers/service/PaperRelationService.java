package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperRelationDto;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.PaperRelation;
import com.dealwithpapers.dealwithpapers.repository.PaperRelationRepository;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperRelationService {
    
    private final PaperRelationRepository paperRelationRepository;
    private final PaperRepository paperRepository;
    
    private static final int MAX_RELATIONS_PER_TYPE = 15;
    private static final double CITATION_WEIGHT = 0.5;
    private static final double INFLUENTIAL_WEIGHT = 0.2;
    private static final double YEAR_WEIGHT = 0.3;
    private static final int BASE_YEAR = 2024; // 基准年份
    
    /**
     * 保存论文引用关系
     */
    @Transactional
    public void savePaperRelations(PaperRelationDto relationDto) {
        Long paperId = relationDto.getPaperId();
        
        // 验证论文是否存在
        Optional<Paper> paperOpt = paperRepository.findById(paperId);
        if (paperOpt.isEmpty()) {
            log.error("论文不存在: {}", paperId);
            return;
        }
        
        // 处理引用关系
        if (relationDto.getReferences() != null) {
            saveRelations(paperId, relationDto.getReferences(), PaperRelation.RelationType.REFERENCES);
        }
        
        // 处理被引用关系
        if (relationDto.getCitations() != null) {
            saveRelations(paperId, relationDto.getCitations(), PaperRelation.RelationType.CITED_BY);
        }
    }
    
    /**
     * 保存指定类型的关系
     */
    private void saveRelations(Long paperId, List<PaperRelationDto.RelationPaper> relations, PaperRelation.RelationType relationType) {
        for (PaperRelationDto.RelationPaper relation : relations) {
            // 检查是否已存在相同关系
            if (isRelationExists(paperId, relation, relationType)) {
                log.debug("关系已存在，跳过: {} -> {}", paperId, relation.getTitle());
                continue;
            }
            
            // 创建新的关系记录
            PaperRelation paperRelation = createPaperRelation(paperId, relation, relationType);
            
            // 检查是否需要删除优先级最低的记录
            ensureMaxRelationsLimit(paperId, relationType);
            
            // 保存关系
            paperRelationRepository.save(paperRelation);
            log.info("保存论文关系: {} -> {}", paperId, relation.getTitle());
        }
    }
    
    /**
     * 检查关系是否已存在
     */
    private boolean isRelationExists(Long paperId, PaperRelationDto.RelationPaper relation, PaperRelation.RelationType relationType) {
        // 优先通过DOI检查
        if (relation.getDoi() != null && !relation.getDoi().trim().isEmpty()) {
            return paperRelationRepository.findBySourcePaperIdAndTargetDoiAndRelationType(paperId, relation.getDoi(), relationType).isPresent();
        }
        
        // 如果没有DOI，通过标题检查
        return paperRelationRepository.findBySourcePaperIdAndTargetTitleAndRelationType(paperId, relation.getTitle(), relationType).isPresent();
    }
    
    /**
     * 创建论文关系对象
     */
    private PaperRelation createPaperRelation(Long paperId, PaperRelationDto.RelationPaper relation, PaperRelation.RelationType relationType) {
        PaperRelation paperRelation = new PaperRelation();
        paperRelation.setSourcePaperId(paperId);
        paperRelation.setRelationType(relationType);
        paperRelation.setTargetTitle(relation.getTitle());
        paperRelation.setTargetDoi(relation.getDoi());
        paperRelation.setTargetYear(relation.getYear());
        paperRelation.setCitationCount(relation.getCitationCount());
        paperRelation.setInfluentialCitationCount(relation.getInfluentialCitationCount());
        paperRelation.setTargetVenue(relation.getVenue());
        paperRelation.setTargetAbstract(relation.getAbstractText());
        paperRelation.setSemanticScholarId(relation.getPaperId());
        
        // 处理作者列表
        if (relation.getAuthors() != null && !relation.getAuthors().isEmpty()) {
            StringBuilder authorsStr = new StringBuilder();
            for (int i = 0; i < relation.getAuthors().size(); i++) {
                if (i > 0) authorsStr.append(", ");
                authorsStr.append(relation.getAuthors().get(i).getName());
            }
            paperRelation.setTargetAuthors(authorsStr.toString());
        }
        
        // 处理引用意图
        if (relation.getIntent() != null && !relation.getIntent().isEmpty()) {
            paperRelation.setCitationIntent(String.join(",", relation.getIntent()));
        }
        
        // 处理开放访问PDF
        if (relation.getOpenAccessPdf() != null && relation.getOpenAccessPdf().getUrl() != null) {
            paperRelation.setOpenAccessUrl(relation.getOpenAccessPdf().getUrl());
        }
        
        // 查找目标论文是否在数据库中
        if (relation.getDoi() != null && !relation.getDoi().trim().isEmpty()) {
            Paper targetPaper = paperRepository.findByDoi(relation.getDoi());
            if (targetPaper != null) {
                paperRelation.setTargetPaperId(targetPaper.getId());
            }
        }
        
        // 计算优先级分数
        paperRelation.setPriorityScore(calculatePriorityScore(relation.getCitationCount(), relation.getInfluentialCitationCount(), relation.getYear()));
        
        return paperRelation;
    }
    
    /**
     * 计算优先级分数
     */
    private double calculatePriorityScore(Integer citationCount, Integer influentialCitationCount, Integer year) {
        // 引用数量分数 (归一化到0-1)
        double citationScore = citationCount != null ? Math.min(citationCount / 1000.0, 1.0) : 0.0;
        
        // 有影响力引用分数 (归一化到0-1)
        double influentialScore = influentialCitationCount != null ? Math.min(influentialCitationCount / 100.0, 1.0) : 0.0;
        
        // 年份分数 (越近期分数越高)
        double yearScore = 0.0;
        if (year != null) {
            int yearDiff = Math.abs(BASE_YEAR - year);
            yearScore = Math.max(0, 1.0 - (yearDiff / 50.0)); // 50年内的论文有效
        }
        
        return CITATION_WEIGHT * citationScore + INFLUENTIAL_WEIGHT * influentialScore + YEAR_WEIGHT * yearScore;
    }
    
    /**
     * 确保关系数量不超过限制
     */
    private void ensureMaxRelationsLimit(Long paperId, PaperRelation.RelationType relationType) {
        long currentCount = paperRelationRepository.countBySourcePaperIdAndRelationType(paperId, relationType);
        
        if (currentCount >= MAX_RELATIONS_PER_TYPE) {
            // 删除优先级最低的记录
            List<PaperRelation> lowestPriorityRelations = paperRelationRepository.findLowestPriorityRelations(paperId, relationType);
            if (!lowestPriorityRelations.isEmpty()) {
                PaperRelation toDelete = lowestPriorityRelations.get(0);
                paperRelationRepository.delete(toDelete);
                log.info("删除低优先级关系: {} -> {}", paperId, toDelete.getTargetTitle());
            }
        }
    }
    
    /**
     * 获取论文的引用关系
     */
    public List<PaperRelation> getPaperReferences(Long paperId) {
        return paperRelationRepository.findBySourcePaperIdAndRelationTypeOrderByPriorityScoreDesc(paperId, PaperRelation.RelationType.REFERENCES);
    }
    
    /**
     * 获取论文的被引用关系
     */
    public List<PaperRelation> getPaperCitations(Long paperId) {
        return paperRelationRepository.findBySourcePaperIdAndRelationTypeOrderByPriorityScoreDesc(paperId, PaperRelation.RelationType.CITED_BY);
    }
    
    /**
     * 获取论文的所有关系
     */
    public List<PaperRelation> getAllPaperRelations(Long paperId) {
        return paperRelationRepository.findBySourcePaperIdAndRelationTypeOrderByPriorityScoreDesc(paperId, null);
    }
    
    /**
     * 删除论文的所有关系
     */
    @Transactional
    public void deletePaperRelations(Long paperId) {
        List<PaperRelation> references = getPaperReferences(paperId);
        List<PaperRelation> citations = getPaperCitations(paperId);
        
        paperRelationRepository.deleteAll(references);
        paperRelationRepository.deleteAll(citations);
        
        log.info("删除论文 {} 的所有关系", paperId);
    }
}