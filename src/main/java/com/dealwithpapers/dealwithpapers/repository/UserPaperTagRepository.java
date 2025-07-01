package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.UserPaperTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPaperTagRepository extends JpaRepository<UserPaperTag, Long> {
    
    // 查找用户的所有标签
    List<UserPaperTag> findByUserId(Long userId);
    
    // 查找指定用户给特定论文添加的所有标签
    List<UserPaperTag> findByUserIdAndPaperId(Long userId, Long paperId);
    
    // 查找用户添加的特定名称的所有标签
    List<UserPaperTag> findByUserIdAndTagName(Long userId, String tagName);
    
    // 查找特定用户对特定论文添加的特定标签
    Optional<UserPaperTag> findByUserIdAndPaperIdAndTagName(Long userId, Long paperId, String tagName);
    
    // 删除特定用户对特定论文的特定标签
    void deleteByUserIdAndPaperIdAndTagName(Long userId, Long paperId, String tagName);
    
    // 删除特定用户对特定论文的所有标签
    void deleteByUserIdAndPaperId(Long userId, Long paperId);
    
    // 统计用户创建的标签数量
    long countByUserId(Long userId);
} 