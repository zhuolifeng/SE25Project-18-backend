package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}")
public class PostLikeController {
    @Autowired
    private PostLikeService postLikeService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户
     * @return 当前用户ID
     */
    private User getCurrentUser() {
        return AuthUtils.getCurrentUser(userRepository);
    }

    @PostMapping("/like")
    public Map<String, Object> like(@PathVariable Long postId) {
        User currentUser = getCurrentUser();
        postLikeService.like(currentUser.getId(), postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "点赞成功");
        return res;
    }

    @PostMapping("/dislike")
    public Map<String, Object> dislike(@PathVariable Long postId) {
        User currentUser = getCurrentUser();
        postLikeService.dislike(currentUser.getId(), postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "点踩成功");
        return res;
    }

    @DeleteMapping("/like")
    public Map<String, Object> cancel(@PathVariable Long postId) {
        User currentUser = getCurrentUser();
        postLikeService.cancel(currentUser.getId(), postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "操作已取消");
        return res;
    }

    @GetMapping("/like-stats")
    public Map<String, Object> likeStats(@PathVariable Long postId) {
        int likes = postLikeService.countLikes(postId);
        int dislikes = postLikeService.countDislikes(postId);
        
        // 获取当前用户的点赞状态
        Integer userType = null;
        try {
            User currentUser = getCurrentUser();
            userType = postLikeService.getUserLikeType(currentUser.getId(), postId);
        } catch (Exception e) {
            // 未登录或其他异常，用户状态为null
        }
        
        Map<String, Object> res = new HashMap<>();
        res.put("likes", likes);
        res.put("dislikes", dislikes);
        res.put("userType", userType); // 1=赞，-1=踩，null=无
        return res;
    }
} 