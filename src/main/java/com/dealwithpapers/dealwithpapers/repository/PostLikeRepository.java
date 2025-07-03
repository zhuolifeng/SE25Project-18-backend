package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.PostLike;
import com.dealwithpapers.dealwithpapers.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserAndPost(User user, Post post);
    
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    
    long countByPostAndType(Post post, int type);
    
    int countByPostIdAndType(Long postId, int type);
    
    List<PostLike> findByUserIdAndType(Long userId, int type);
    
    /**
     * 根据帖子ID删除所有点赞/点踩记录
     * @param postId 帖子ID
     * @return 删除的记录数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = ?1")
    int deleteByPostId(Long postId);
    
    /**
     * 根据用户ID和点赞/点踩记录ID删除
     * @param userId 用户ID
     * @param likeId 点赞/点踩记录ID
     * @return 删除的记录数量
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = ?1 AND pl.id = ?2")
    int deleteByUserIdAndId(Long userId, Long likeId);
} 