package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.CommentDTO;
import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import java.util.List;
import java.util.Map;

public interface CommentService {
    CommentDTO addComment(CommentDTO commentDTO);
    List<CommentDTO> getCommentsByPostId(Long postId);
    
    /**
     * 获取用户评论过的所有帖子
     * @param userId 用户ID
     * @return 帖子DTO列表
     */
    List<PostDTO> getUserCommentedPosts(Long userId);
    
    /**
     * 获取用户的所有评论（带帖子信息）
     * @param userId 用户ID
     * @return 包含评论和帖子信息的列表
     */
    List<Map<String, Object>> getUserComments(Long userId);
    
    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID（用于权限检查）
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);
} 