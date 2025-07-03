package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.PostFavorite;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.PostFavoriteRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostFavoriteService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostFavoriteServiceImpl implements PostFavoriteService {

    @Autowired
    private PostFavoriteRepository postFavoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Override
    @Transactional
    public void favoritePost(Long userId, Long postId) {
        // 调试日志
        System.out.println("开始收藏帖子，用户ID: " + userId + ", 帖子ID: " + postId);
        
        try {
            // 检查帖子和用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new EntityNotFoundException("帖子不存在"));
            
            System.out.println("用户和帖子都存在，用户: " + user.getUsername() + ", 帖子: " + post.getTitle());

            // 检查是否已经收藏
            boolean exists = postFavoriteRepository.existsByUserIdAndPostId(userId, postId);
            System.out.println("检查是否已收藏：" + exists);
            
            if (exists) {
                System.out.println("帖子已经收藏，不重复操作");
                return; // 已经收藏，不重复操作
            }

            // 创建收藏记录
            PostFavorite favorite = new PostFavorite();
            favorite.setUser(user);
            favorite.setPost(post);
            favorite.setCreateTime(LocalDateTime.now());
            PostFavorite saved = postFavoriteRepository.save(favorite);
            System.out.println("收藏成功，收藏ID: " + saved.getId());
        } catch (Exception e) {
            System.err.println("收藏帖子出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public void unfavoritePost(Long userId, Long postId) {
        // 调试日志
        System.out.println("开始取消收藏帖子，用户ID: " + userId + ", 帖子ID: " + postId);
        
        try {
            // 检查是否已经收藏
            PostFavorite favorite = postFavoriteRepository.findByUserIdAndPostId(userId, postId)
                    .orElse(null);
            
            if (favorite != null) {
                System.out.println("找到收藏记录，收藏ID: " + favorite.getId() + "，准备删除");
                postFavoriteRepository.delete(favorite);
                System.out.println("已删除收藏记录");
            } else {
                System.out.println("未找到收藏记录，无需删除");
            }
        } catch (Exception e) {
            System.err.println("取消收藏帖子出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        // 调试日志
        System.out.println("检查帖子是否已收藏，用户ID: " + userId + ", 帖子ID: " + postId);
        
        try {
            boolean favorited = postFavoriteRepository.existsByUserIdAndPostId(userId, postId);
            System.out.println("收藏检查结果: " + favorited);
            return favorited;
        } catch (Exception e) {
            System.err.println("检查帖子收藏状态出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<PostDTO> getUserFavorites(Long userId) {
        // 调试日志
        System.out.println("获取用户收藏的帖子，用户ID: " + userId);
        
        try {
            // 获取用户收藏的所有帖子
            List<PostFavorite> favorites = postFavoriteRepository.findByUserId(userId);
            System.out.println("找到收藏记录数: " + favorites.size());
            
            // 转换为DTO对象
            List<PostDTO> result = favorites.stream()
                    .map(favorite -> {
                        Post post = favorite.getPost();
                        PostDTO dto = new PostDTO();
                        dto.setId(post.getId());
                        dto.setTitle(post.getTitle());
                        dto.setContent(post.getContent());
                        dto.setCategory(post.getCategory());
                        dto.setType(post.getType());
                        dto.setAuthorId(post.getAuthor().getId());
                        dto.setAuthorName(post.getAuthor().getUsername());
                        dto.setCreateTime(post.getCreateTime());
                        dto.setPostTags(post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet()));
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            System.out.println("转换为DTO后的记录数: " + result.size());
            
            // 打印每个收藏的帖子标题
            for (int i = 0; i < result.size(); i++) {
                System.out.println("收藏帖子 #" + (i + 1) + ": " + result.get(i).getTitle());
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("获取用户收藏帖子出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Page<PostDTO> getUserFavorites(Long userId, Pageable pageable) {
        // 暂时不实现分页查询，后续可扩展
        throw new UnsupportedOperationException("分页查询暂未实现");
    }

    @Override
    public long countFavorites(Long postId) {
        return postFavoriteRepository.countByPostId(postId);
    }
} 