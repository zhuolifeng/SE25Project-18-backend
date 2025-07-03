package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserFavoriteService {
    
    /**
     * 收藏论文
     * @param userId 用户ID
     * @param paperId 论文ID
     * @return 是否成功
     */
    boolean addFavorite(Long userId, Long paperId);
    
    /**
     * 取消收藏
     * @param userId 用户ID
     * @param paperId 论文ID
     * @return 是否成功
     */
    boolean removeFavorite(Long userId, Long paperId);
    
    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 论文列表
     */
    Page<PaperDTO> getUserFavorites(Long userId, Pageable pageable);
    
    /**
     * 检查用户是否已收藏某论文
     * @param userId 用户ID
     * @param paperId 论文ID
     * @return 是否已收藏
     */
    boolean checkIsFavorite(Long userId, Long paperId);
} 