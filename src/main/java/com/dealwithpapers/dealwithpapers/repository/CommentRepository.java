package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreateTimeAsc(Long postId);
    
    /**
     * 查询用户发表的所有评论
     * @param userId 用户ID
     * @return 评论列表
     */
    List<Comment> findByUserId(Long userId);
    
    /**
     * 统计某个帖子的评论数
     * @param postId 帖子ID
     * @return 评论数
     */
    long countByPostId(Long postId);
    
    /**
     * 根据帖子ID删除所有评论
     * @param postId 帖子ID
     * @return 删除的评论数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id = ?1")
    int deleteByPostId(Long postId);
    
    /**
     * 根据用户ID和评论ID删除评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 删除的评论数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.user.id = ?1 AND c.id = ?2")
    int deleteByUserIdAndId(Long userId, Long commentId);
} 