package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    /**
     * 查找用户是否已关注另一用户
     * 
     * @param follower 关注者
     * @param following 被关注者
     * @return 关注关系（如果存在）
     */
    Optional<UserFollow> findByFollowerAndFollowing(User follower, User following);
    
    /**
     * 查找用户的所有关注
     * 
     * @param follower 关注者
     * @return 该用户关注的用户列表
     */
    List<UserFollow> findByFollower(User follower);
    
    /**
     * 查找用户的所有粉丝
     * 
     * @param following 被关注者
     * @return 关注该用户的用户列表
     */
    List<UserFollow> findByFollowing(User following);
    
    /**
     * 统计用户关注的数量
     * 
     * @param follower 关注者
     * @return 关注数量
     */
    long countByFollower(User follower);
    
    /**
     * 统计用户的粉丝数量
     * 
     * @param following 被关注者
     * @return 粉丝数量
     */
    long countByFollowing(User following);
    
    /**
     * 删除用户之间的关注关系
     * 
     * @param follower 关注者
     * @param following 被关注者
     */
    void deleteByFollowerAndFollowing(User follower, User following);
} 