package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.UserPaperTagDTO;

import java.util.List;
import java.util.Map;

public interface UserPaperTagService {
    
    /**
     * 添加用户自定义标签
     * 
     * @param userId 用户ID
     * @param paperId 论文ID
     * @param tagName 标签名称
     * @param tagColor 标签颜色
     * @return 添加的标签信息
     */
    UserPaperTagDTO addTag(Long userId, Long paperId, String tagName, String tagColor);
    
    /**
     * 删除用户标签
     * 
     * @param userId 用户ID
     * @param paperId 论文ID
     * @param tagName 标签名称
     * @return 是否成功删除
     */
    boolean removeTag(Long userId, Long paperId, String tagName);
    
    /**
     * 获取用户给特定论文添加的所有标签
     * 
     * @param userId 用户ID
     * @param paperId 论文ID
     * @return 标签列表
     */
    List<UserPaperTagDTO> getTagsByPaper(Long userId, Long paperId);
    
    /**
     * 获取用户创建的所有标签及对应的论文
     * 
     * @param userId 用户ID
     * @return 标签及论文信息
     */
    Map<String, List<UserPaperTagDTO>> getAllUserTags(Long userId);
    
    /**
     * 获取使用特定标签名称的所有论文
     * 
     * @param userId 用户ID
     * @param tagName 标签名称
     * @return 论文列表
     */
    List<UserPaperTagDTO> getPapersByTag(Long userId, String tagName);
    
    /**
     * 更新标签颜色
     * 
     * @param userId 用户ID
     * @param paperId 论文ID
     * @param tagName 标签名称
     * @param newColor 新的颜色
     * @return 更新后的标签
     */
    UserPaperTagDTO updateTagColor(Long userId, Long paperId, String tagName, String newColor);
} 