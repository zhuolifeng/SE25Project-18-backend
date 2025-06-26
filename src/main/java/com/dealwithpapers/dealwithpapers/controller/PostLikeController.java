package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}")
public class PostLikeController {
    @Autowired
    private PostLikeService postLikeService;

    @PostMapping("/like")
    public Map<String, Object> like(@PathVariable Long postId, @RequestParam Long userId) {
        postLikeService.like(userId, postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "点赞成功");
        return res;
    }

    @PostMapping("/dislike")
    public Map<String, Object> dislike(@PathVariable Long postId, @RequestParam Long userId) {
        postLikeService.dislike(userId, postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "点踩成功");
        return res;
    }

    @DeleteMapping("/like")
    public Map<String, Object> cancel(@PathVariable Long postId, @RequestParam Long userId) {
        postLikeService.cancel(userId, postId);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "操作已取消");
        return res;
    }

    @GetMapping("/like-stats")
    public Map<String, Object> likeStats(@PathVariable Long postId, @RequestParam(required = false) Long userId) {
        int likes = postLikeService.countLikes(postId);
        int dislikes = postLikeService.countDislikes(postId);
        Integer userType = userId != null ? postLikeService.getUserLikeType(userId, postId) : null;
        Map<String, Object> res = new HashMap<>();
        res.put("likes", likes);
        res.put("dislikes", dislikes);
        res.put("userType", userType); // 1=赞，-1=踩，null=无
        return res;
    }
} 