package com.dealwithpapers.dealwithpapers.service;

public interface PostLikeService {
    void like(Long userId, Long postId);
    void dislike(Long userId, Long postId);
    void cancel(Long userId, Long postId);
    int countLikes(Long postId);
    int countDislikes(Long postId);
    Integer getUserLikeType(Long userId, Long postId); // 1=赞，-1=踩，null=无
} 