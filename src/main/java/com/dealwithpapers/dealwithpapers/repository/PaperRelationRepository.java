package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.PaperRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRelationRepository extends JpaRepository<PaperRelation, Long> {
    
    /**
     * 根据源论文ID和关系类型查找关系
     */
    List<PaperRelation> findBySourcePaperIdAndRelationType(Long sourcePaperId, PaperRelation.RelationType relationType);
    
    /**
     * 根据源论文ID和关系类型查找关系，按优先级排序
     */
    List<PaperRelation> findBySourcePaperIdAndRelationTypeOrderByPriorityScoreDesc(Long sourcePaperId, PaperRelation.RelationType relationType);
    
    /**
     * 根据源论文ID、目标DOI和关系类型查找关系
     */
    Optional<PaperRelation> findBySourcePaperIdAndTargetDoiAndRelationType(Long sourcePaperId, String targetDoi, PaperRelation.RelationType relationType);
    
    /**
     * 根据源论文ID、目标标题和关系类型查找关系（用于没有DOI的情况）
     */
    Optional<PaperRelation> findBySourcePaperIdAndTargetTitleAndRelationType(Long sourcePaperId, String targetTitle, PaperRelation.RelationType relationType);
    
    /**
     * 统计某篇论文的引用关系数量
     */
    @Query("SELECT COUNT(pr) FROM PaperRelation pr WHERE pr.sourcePaperId = :paperId AND pr.relationType = :relationType")
    long countBySourcePaperIdAndRelationType(@Param("paperId") Long paperId, @Param("relationType") PaperRelation.RelationType relationType);
    
    /**
     * 获取某篇论文的引用关系，按优先级排序（用于删除优先级最低的记录）
     */
    List<PaperRelation> findBySourcePaperIdAndRelationTypeOrderByPriorityScoreAsc(Long sourcePaperId, PaperRelation.RelationType relationType);
    
    /**
     * 根据目标论文ID查找所有引用它的论文
     */
    List<PaperRelation> findByTargetPaperIdAndRelationType(Long targetPaperId, PaperRelation.RelationType relationType);
    
    /**
     * 根据DOI查找论文关系
     */
    List<PaperRelation> findByTargetDoi(String targetDoi);
    
    /**
     * 删除源论文的特定类型关系中优先级最低的记录
     */
    @Query("SELECT pr FROM PaperRelation pr WHERE pr.sourcePaperId = :paperId AND pr.relationType = :relationType ORDER BY pr.priorityScore ASC")
    List<PaperRelation> findLowestPriorityRelations(@Param("paperId") Long paperId, @Param("relationType") PaperRelation.RelationType relationType);
}