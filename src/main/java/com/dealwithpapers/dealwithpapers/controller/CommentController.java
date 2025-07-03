package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.CommentDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.CommentService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户
     * @return 当前用户
     */
    private User getCurrentUser() {
        try {
            return AuthUtils.getCurrentUser(userRepository);
        } catch (IllegalStateException e) {
            // 打印日志但不抛出异常
            System.out.println("CommentController - 获取当前用户失败: " + e.getMessage());
            return null;
        }
    }

    @PostMapping
    public CommentDTO addComment(@RequestBody CommentDTO commentDTO) {
        return commentService.addComment(commentDTO);
    }

    @GetMapping("/{postId}")
    public List<CommentDTO> getCommentsByPost(@PathVariable Long postId) {
        return commentService.getCommentsByPostId(postId);
    }
    
    /**
     * 获取当前登录用户的所有评论（带帖子信息）
     * @return 用户的评论列表
     */
    @GetMapping("/user/comments")
    public Map<String, Object> getUserComments() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再查看评论");
                response.put("data", List.of());
                return response;
            }
            
            List<Map<String, Object>> comments = commentService.getUserComments(currentUser.getId());
            
            response.put("success", true);
            response.put("data", comments);
            
            // 调试信息
            System.out.println("获取用户评论成功, 用户ID: " + currentUser.getId() + ", 评论数量: " + comments.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户评论失败: " + e.getMessage());
            response.put("data", List.of());
            
            // 调试信息
            System.out.println("获取用户评论失败: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
    
    /**
     * 删除评论
     * @param commentId 评论ID
     * @return 响应结果
     */
    @DeleteMapping("/{commentId}")
    public Map<String, Object> deleteComment(@PathVariable Long commentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "请先登录后再删除评论");
                return response;
            }
            
            boolean deleted = commentService.deleteComment(commentId, currentUser.getId());
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "评论删除成功");
                System.out.println("评论删除成功, 用户ID: " + currentUser.getId() + ", 评论ID: " + commentId);
            } else {
                response.put("success", false);
                response.put("message", "评论删除失败，可能无权限或评论不存在");
                System.out.println("评论删除失败, 用户ID: " + currentUser.getId() + ", 评论ID: " + commentId);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除评论失败: " + e.getMessage());
            
            System.out.println("删除评论失败: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
} 