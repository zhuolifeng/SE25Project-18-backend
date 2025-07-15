package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.PostLike;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.PostLikeRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostLikeServiceImpl implements PostLikeService {
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @Override
    @Transactional
    public void like(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if (existingLike.isPresent()) {
            PostLike postLike = existingLike.get();
            postLike.setType(1);
            postLikeRepository.save(postLike);
        } else {
            PostLike postLike = new PostLike();
            postLike.setUser(user);
            postLike.setPost(post);
            postLike.setType(1);
            postLike.setCreateTime(LocalDateTime.now());
            postLikeRepository.save(postLike);
        }
    }

    @Override
    @Transactional
    public void dislike(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if (existingLike.isPresent()) {
            PostLike postLike = existingLike.get();
            postLike.setType(-1);
            postLikeRepository.save(postLike);
        } else {
            PostLike postLike = new PostLike();
            postLike.setUser(user);
            postLike.setPost(post);
            postLike.setType(-1);
            postLike.setCreateTime(LocalDateTime.now());
            postLikeRepository.save(postLike);
        }
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long postId) {
        postLikeRepository.findByUserIdAndPostId(userId, postId)
                .ifPresent(postLikeRepository::delete);
    }

    @Override
    public int countLikes(Long postId) {
        return postLikeRepository.countByPostIdAndType(postId, 1);
    }

    @Override
    public int countDislikes(Long postId) {
        return postLikeRepository.countByPostIdAndType(postId, -1);
    }

    @Override
    public Integer getUserLikeType(Long userId, Long postId) {
        return postLikeRepository.findByUserIdAndPostId(userId, postId)
                .map(PostLike::getType)
                .orElse(null);
    }
    
    @Override
    public List<PostDTO> getUserLikedPosts(Long userId) {
        // 获取用户点赞的帖子
        List<PostLike> likes = postLikeRepository.findByUserIdAndType(userId, 1);
        
        // 转换为DTO
        return likes.stream()
                .map(like -> {
                    Post post = like.getPost();
                    PostDTO dto = new PostDTO();
                    dto.setId(post.getId());
                    dto.setTitle(post.getTitle());
                    dto.setContent(post.getContent());
                    dto.setCategory(post.getCategory());
                    dto.setType(post.getType());
                    dto.setAuthorId(post.getAuthor().getId());
                    dto.setAuthorName(post.getAuthor().getUsername());
                    dto.setAuthorAvatar(post.getAuthor().getAvatarUrl()); // 添加作者头像URL
                    dto.setCreateTime(post.getCreateTime());
                    dto.setPostTags(post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PostDTO> getUserDislikedPosts(Long userId) {
        // 获取用户点踩的帖子
        List<PostLike> dislikes = postLikeRepository.findByUserIdAndType(userId, -1);
        
        // 转换为DTO
        return dislikes.stream()
                .map(dislike -> {
                    Post post = dislike.getPost();
                    PostDTO dto = new PostDTO();
                    dto.setId(post.getId());
                    dto.setTitle(post.getTitle());
                    dto.setContent(post.getContent());
                    dto.setCategory(post.getCategory());
                    dto.setType(post.getType());
                    dto.setAuthorId(post.getAuthor().getId());
                    dto.setAuthorName(post.getAuthor().getUsername());
                    dto.setAuthorAvatar(post.getAuthor().getAvatarUrl()); // 添加作者头像URL
                    dto.setCreateTime(post.getCreateTime());
                    dto.setPostTags(post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
} 