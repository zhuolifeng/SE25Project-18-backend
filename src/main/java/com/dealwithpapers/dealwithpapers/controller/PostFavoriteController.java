package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostFavoriteService;
import com.dealwithpapers.dealwithpapers.service.PostService;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts/favorites")
public class PostFavoriteController {

    @Autowired
    private PostFavoriteService postFavoriteService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private PostLikeService postLikeService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户
     * @return 当前用户ID
     */
    private User getCurrentUser() {
        try {
            return AuthUtils.getCurrentUser(userRepository);
        } catch (IllegalStateException e) {
            // 打印日志但不抛出异常
            System.out.println("PostFavoriteController - 获取当前用户失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 收藏帖子
     * @param postId 帖子ID
     * @return 响应结果
     */
    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestParam Long postId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再收藏");
                return response;
            }
            
            postFavoriteService.favoritePost(currentUser.getId(), postId);
            response.put("success", true);
            response.put("message", "收藏成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "收藏失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 取消收藏帖子
     * @param postId 帖子ID
     * @return 响应结果
     */
    @DeleteMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestParam Long postId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再取消收藏");
                return response;
            }
            
            postFavoriteService.unfavoritePost(currentUser.getId(), postId);
            response.put("success", true);
            response.put("message", "取消收藏成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "取消收藏失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 检查帖子是否已收藏
     * @param postId 帖子ID
     * @return 响应结果
     */
    @GetMapping("/check")
    public Map<String, Object> checkFavorite(@RequestParam Long postId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先检查用户是否已登录
            if (!AuthUtils.isUserLoggedIn()) {
                response.put("success", false);
                response.put("message", "请先登录后再查看收藏状态");
                response.put("favorited", false);
                return response;
            }
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再查看收藏状态");
                response.put("favorited", false);
                return response;
            }
            
            boolean favorited = postFavoriteService.isFavorited(currentUser.getId(), postId);
            response.put("success", true);
            response.put("favorited", favorited);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "检查收藏状态失败: " + e.getMessage());
            response.put("favorited", false);
        }
        return response;
    }

    /**
     * 获取用户收藏的帖子列表
     * @return 响应结果
     */
    @GetMapping("/list")
    public Map<String, Object> getUserFavorites() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先检查用户是否已登录
            if (!AuthUtils.isUserLoggedIn()) {
                response.put("success", false);
                response.put("message", "请先登录后再查看收藏列表");
                response.put("data", List.of());
                return response;
            }
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再查看收藏列表");
                response.put("data", List.of());
                return response;
            }
            
            List<PostDTO> favorites = postFavoriteService.getUserFavorites(currentUser.getId());
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = favorites.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", 0); // 暂无评论统计
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().toString() : "");
                return item;
            }).collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", result);
            
            // 调试信息
            System.out.println("获取用户收藏帖子成功, 用户ID: " + currentUser.getId() + ", 收藏数量: " + result.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取收藏列表失败: " + e.getMessage());
            response.put("data", List.of());
            // 调试信息
            System.out.println("获取用户收藏帖子失败: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
} 