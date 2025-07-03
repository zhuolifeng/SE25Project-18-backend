package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import java.util.List;

public interface PostLikeService {
    void like(Long userId, Long postId);
    void dislike(Long userId, Long postId);
    void cancel(Long userId, Long postId);
    int countLikes(Long postId);
    int countDislikes(Long postId);
    Integer getUserLikeType(Long userId, Long postId); // 1=赞，-1=踩，null=无
    
    /**
     * 获取用户点赞的帖子列表
     * @param userId 用户ID
     * @return 帖子列表
     */
    List<PostDTO> getUserLikedPosts(Long userId);
    
    /**
     * 获取用户点踩的帖子列表
     * @param userId 用户ID
     * @return 帖子列表
     */
    List<PostDTO> getUserDislikedPosts(Long userId);
} 