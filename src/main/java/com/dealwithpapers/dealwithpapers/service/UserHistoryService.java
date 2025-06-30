package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.UserSearchHistoryDTO;
import com.dealwithpapers.dealwithpapers.dto.UserViewHistoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserHistoryService {
    
    // ===== 搜索历史相关方法 =====
    
    // 保存用户搜索记录
    UserSearchHistoryDTO saveSearchHistory(Long userId, String searchText);
    
    // 获取用户的搜索历史（分页）
    Page<UserSearchHistoryDTO> getUserSearchHistory(Long userId, Pageable pageable);
    
    // 获取用户最近的搜索历史（限制数量）
    List<UserSearchHistoryDTO> getRecentSearchHistory(Long userId, int limit);
    
    // 获取用户最近的不重复搜索词
    List<String> getDistinctSearchTerms(Long userId, int limit);
    
    // 删除指定的搜索记录
    void deleteSearchHistory(Long id);
    
    // 清空用户的所有搜索历史
    void clearUserSearchHistory(Long userId);
    
    // 限制用户搜索历史记录数量（保留最新的50条）
    void limitUserSearchHistory(Long userId, int maxRecords);
    
    // ===== 浏览历史相关方法 =====
    
    // 保存用户浏览记录
    UserViewHistoryDTO saveViewHistory(Long userId, Long paperId);
    
    // 获取用户的浏览历史（分页）
    Page<UserViewHistoryDTO> getUserViewHistory(Long userId, Pageable pageable);
    
    // 获取用户最近的浏览历史（限制数量）
    List<UserViewHistoryDTO> getRecentViewHistory(Long userId, int limit);
    
    // 获取论文的浏览次数
    long getPaperViewCount(Long paperId);
    
    // 删除指定的浏览记录
    void deleteViewHistory(Long id);
    
    // 清空用户的所有浏览历史
    void clearUserViewHistory(Long userId);
    
    // 限制用户浏览历史记录数量（保留最新的50条）
    void limitUserViewHistory(Long userId, int maxRecords);
} 