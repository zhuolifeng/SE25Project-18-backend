package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.UserViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserViewHistoryRepository extends JpaRepository<UserViewHistory, Long> {
    
    // 查找用户的所有浏览历史（分页）
    Page<UserViewHistory> findByUserIdOrderByViewTimeDesc(Long userId, Pageable pageable);
    
    // 查找用户的所有浏览历史
    List<UserViewHistory> findByUserIdOrderByViewTimeDesc(Long userId);
    
    // 限制数量的浏览历史
    List<UserViewHistory> findTop10ByUserIdOrderByViewTimeDesc(Long userId);
    
    // 查找特定论文的浏览记录
    List<UserViewHistory> findByPaperIdOrderByViewTimeDesc(String paperId, Pageable pageable);
    
    // 检查用户是否浏览过某篇论文
    boolean existsByUserIdAndPaperId(Long userId, String paperId);
    
    // 获取论文的浏览次数
    long countByPaperId(String paperId);
    
    // 获取用户的浏览次数
    long countByUserId(Long userId);
    
    // 获取热门论文（被浏览最多的论文）
    @Query(value = "SELECT paper_id, COUNT(*) as view_count FROM user_view_history " +
                   "GROUP BY paper_id ORDER BY view_count DESC LIMIT ?1", nativeQuery = true)
    List<Object[]> findMostViewedPapers(int limit);
    
    // 删除指定用户的所有浏览历史
    void deleteByUserId(Long userId);
} 