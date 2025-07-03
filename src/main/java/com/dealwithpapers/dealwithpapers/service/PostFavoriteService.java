package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostFavoriteService {
    
    /**
     * 收藏帖子
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void favoritePost(Long userId, Long postId);
    
    /**
     * 取消收藏帖子
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    void unfavoritePost(Long userId, Long postId);
    
    /**
     * 检查用户是否已收藏帖子
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否已收藏
     */
    boolean isFavorited(Long userId, Long postId);
    
    /**
     * 获取用户收藏的帖子列表
     * @param userId 用户ID
     * @return 帖子列表
     */
    List<PostDTO> getUserFavorites(Long userId);
    
    /**
     * 获取用户收藏的帖子（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页帖子列表
     */
    Page<PostDTO> getUserFavorites(Long userId, Pageable pageable);
    
    /**
     * 获取帖子的收藏数量
     * @param postId 帖子ID
     * @return 收藏数量
     */
    long countFavorites(Long postId);
} 