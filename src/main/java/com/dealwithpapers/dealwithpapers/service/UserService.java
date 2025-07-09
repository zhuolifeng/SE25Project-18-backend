package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.UserUpdateDTO;
import com.dealwithpapers.dealwithpapers.dto.PasswordUpdateDTO;
import java.util.Map;
import java.util.List;

public interface UserService {
    UserResponseDTO register(UserRegisterDTO registerDTO);
    UserResponseDTO login(UserLoginDTO loginDTO);
    UserResponseDTO getCurrentUser();
    void logout();
    UserResponseDTO updateUserInfo(UserUpdateDTO updateDTO);
    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);
    
    /**
     * 获取用户公开信息
     * @param userId 用户ID
     * @return 用户公开信息
     */
    Map<String, Object> getUserPublicProfile(Long userId);
    
    /**
     * 关注用户
     * @param followingId 要关注的用户ID
     * @return 操作结果
     */
    Map<String, Object> followUser(Long followingId);
    
    /**
     * 取消关注用户
     * @param followingId 要取消关注的用户ID
     * @return 操作结果
     */
    Map<String, Object> unfollowUser(Long followingId);
    
    /**
     * 获取用户关注的用户列表
     * @param userId 用户ID
     * @return 关注的用户列表
     */
    List<Map<String, Object>> getFollowingList(Long userId);
    
    /**
     * 获取用户的粉丝列表
     * @param userId 用户ID
     * @return 粉丝列表
     */
    List<Map<String, Object>> getFollowersList(Long userId);
    
    /**
     * 检查当前用户是否关注了指定用户
     * @param userId 要检查的用户ID
     * @return 是否已关注
     */
    boolean isFollowing(Long userId);
} 