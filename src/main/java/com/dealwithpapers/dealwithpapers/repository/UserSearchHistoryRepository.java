package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.UserSearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {
    
    // 查找用户的所有搜索历史（分页）
    Page<UserSearchHistory> findByUserIdOrderBySearchTimeDesc(Long userId, Pageable pageable);
    
    // 查找用户的所有搜索历史
    List<UserSearchHistory> findByUserIdOrderBySearchTimeDesc(Long userId);
    
    // 限制数量的搜索历史
    List<UserSearchHistory> findTop10ByUserIdOrderBySearchTimeDesc(Long userId);
    
    // 根据搜索内容模糊查询
    Page<UserSearchHistory> findByUserIdAndSearchTextContainingOrderBySearchTimeDesc(Long userId, String keyword, Pageable pageable);
    
    // 获取用户最近的不重复搜索词
    @Query(value = "SELECT DISTINCT search_text FROM user_search_history " +
                   "WHERE user_id = ?1 " +
                   "ORDER BY search_time DESC LIMIT ?2", nativeQuery = true)
    List<String> findDistinctSearchTextByUserId(Long userId, int limit);
    
    // 删除指定用户的所有搜索历史
    void deleteByUserId(Long userId);
} 