package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    
    /**
     * 根据用户ID和论文ID查询收藏记录
     */
    Optional<UserFavorite> findByUserIdAndPaperId(Long userId, Long paperId);
    
    /**
     * 根据用户ID查询该用户的所有收藏，分页
     */
    Page<UserFavorite> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID和论文ID删除收藏记录
     */
    void deleteByUserIdAndPaperId(Long userId, Long paperId);
    
    /**
     * 检查用户是否已收藏某篇论文
     */
    boolean existsByUserIdAndPaperId(Long userId, Long paperId);
} 