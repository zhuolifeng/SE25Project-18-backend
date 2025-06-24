package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, String> {
    
    // 通过ID查找论文（精确匹配）
    Paper findById(String id);
    
    // 通过标题查找论文（模糊匹配）
    List<Paper> findByTitleContainingIgnoreCase(String title);
    
    // 通过作者查找论文（模糊匹配）
    @Query("SELECT p FROM Paper p JOIN p.authors a WHERE LOWER(a) LIKE LOWER(CONCAT('%', :author, '%'))")
    List<Paper> findByAuthorContainingIgnoreCase(@Param("author") String author);
    
    // 通过摘要搜索
    List<Paper> findByAbstractTextContainingIgnoreCase(String abstractText);
    
    // 通过会议名称搜索
    List<Paper> findByConferenceContainingIgnoreCase(String conference);
    
    // 通过类别搜索
    List<Paper> findByCategoryContainingIgnoreCase(String category);
    
    // 综合搜索（ID、标题、作者、摘要、会议、类别）
    @Query("SELECT DISTINCT p FROM Paper p LEFT JOIN p.authors a WHERE " +
           "p.id = :searchTerm OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.conference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Paper> searchByTerm(@Param("searchTerm") String searchTerm);
    
    // 通过年份搜索（单独方法）
    @Query("SELECT p FROM Paper p WHERE FUNCTION('YEAR', p.publishDate) = :year")
    List<Paper> findByPublishYear(@Param("year") Integer year);
} 