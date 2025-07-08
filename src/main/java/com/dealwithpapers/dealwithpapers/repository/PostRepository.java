package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:title%")
    List<Post> findByTitleContaining(@Param("title") String title);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN p.author a WHERE " +
           "CAST(p.id AS string) = :searchTerm OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Post> searchByTerm(@Param("searchTerm") String searchTerm);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName")
    List<Post> findByTagName(@Param("tagName") String tagName);

    @Query("SELECT p FROM Post p WHERE (:type IS NULL OR p.type = :type) AND (:category IS NULL OR p.category = :category) AND p.status = 1")
    List<Post> findByTypeAndCategory(@Param("type") String type, @Param("category") String category);

    // 统计用户发布的帖子数量
    int countByAuthorId(Long authorId);
    
    // 根据论文ID查找相关帖子(包括主要论文和关联论文)
    @Query("SELECT p FROM Post p LEFT JOIN p.relatedPapers rp WHERE p.paper.id = :paperId OR rp.id = :paperId")
    List<Post> findByPaperIdOrRelatedPapersId(@Param("paperId") Long paperId);
} 