package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    
    Optional<PostFavorite> findByUserIdAndPostId(Long userId, Long postId);
    
    @Transactional
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    @Query("SELECT COUNT(pf) > 0 FROM PostFavorite pf WHERE pf.user.id = ?1 AND pf.post.id = ?2")
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    List<PostFavorite> findByUserId(Long userId);
    
    long countByPostId(Long postId);
    
    /**
     * 根据帖子ID删除所有收藏记录
     * @param postId 帖子ID
     * @return 删除的记录数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostFavorite pf WHERE pf.post.id = ?1")
    int deleteByPostId(Long postId);
    
    /**
     * 根据用户ID和收藏记录ID删除
     * @param userId 用户ID
     * @param favoriteId 收藏记录ID
     * @return 删除的记录数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostFavorite pf WHERE pf.user.id = ?1 AND pf.id = ?2")
    int deleteByUserIdAndId(Long userId, Long favoriteId);
} 