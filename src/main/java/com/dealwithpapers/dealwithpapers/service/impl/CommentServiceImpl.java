package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.CommentDTO;
import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.Comment;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.CommentRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public CommentDTO addComment(CommentDTO commentDTO) {
        Comment comment = new Comment();
        comment.setContent(commentDTO.getContent());
        comment.setCreateTime(LocalDateTime.now());
        Post post = postRepository.findById(commentDTO.getPostId()).orElseThrow();
        comment.setPost(post);
        User user = userRepository.findById(commentDTO.getUserId()).orElseThrow();
        comment.setUser(user);
        if (commentDTO.getParentId() != null) {
            Comment parent = commentRepository.findById(commentDTO.getParentId()).orElseThrow();
            comment.setParent(parent);
        }
        Comment saved = commentRepository.save(comment);
        return toDTO(saved);
    }

    @Override
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreateTimeAsc(postId);
        // 构建树形结构
        Map<Long, CommentDTO> map = new HashMap<>();
        List<CommentDTO> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentDTO dto = toDTO(c);
            map.put(dto.getId(), dto);
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentDTO parent = map.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(dto);
                }
            }
        }
        return roots;
    }

    @Override
    public List<PostDTO> getUserCommentedPosts(Long userId) {
        // 查找用户所有的评论
        List<Comment> comments = commentRepository.findByUserId(userId);
        
        // 获取用户评论过的帖子ID列表（去重）
        Set<Long> postIds = comments.stream()
                .map(comment -> comment.getPost().getId())
                .collect(Collectors.toSet());
        
        // 查询并转换为PostDTO
        return postIds.stream()
                .map(postId -> {
                    Post post = postRepository.findById(postId).orElse(null);
                    if (post == null) return null;
                    
                    // 转换为DTO
                    PostDTO dto = new PostDTO();
                    dto.setId(post.getId());
                    dto.setTitle(post.getTitle());
                    dto.setContent(post.getContent());
                    dto.setCategory(post.getCategory());
                    dto.setType(post.getType());
                    dto.setAuthorId(post.getAuthor().getId());
                    dto.setAuthorName(post.getAuthor().getUsername());
                    dto.setCreateTime(post.getCreateTime());
                    
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    private CommentDTO toDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setPostId(comment.getPost().getId());
        dto.setUserId(comment.getUser().getId());
        dto.setUserName(comment.getUser().getUsername());
        dto.setAvatar(comment.getUser().getAvatarUrl());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setCreateTime(comment.getCreateTime());
        return dto;
    }
    
    @Override
    public List<Map<String, Object>> getUserComments(Long userId) {
        // 查找用户所有的评论
        List<Comment> comments = commentRepository.findByUserId(userId);
        
        // 转换为包含评论和帖子信息的列表
        return comments.stream().map(comment -> {
            Map<String, Object> result = new HashMap<>();
            
            // 评论信息
            result.put("id", comment.getId());
            result.put("content", comment.getContent());
            result.put("createTime", comment.getCreateTime());
            result.put("avatar", comment.getUser().getAvatarUrl());
            
            // 帖子信息
            Post post = comment.getPost();
            if (post != null) {
                result.put("postId", post.getId());
                result.put("postTitle", post.getTitle());
                result.put("postAuthor", post.getAuthor() != null ? post.getAuthor().getUsername() : "未知");
            }
            
            return result;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        try {
            // 检查评论是否存在
            Optional<Comment> optionalComment = commentRepository.findById(commentId);
            if (optionalComment.isEmpty()) {
                System.out.println("评论不存在，ID: " + commentId);
                return false;
            }
            
            Comment comment = optionalComment.get();
            
            // 验证评论是否属于该用户
            if (!comment.getUser().getId().equals(userId)) {
                System.out.println("无权删除此评论，评论用户ID: " + comment.getUser().getId() + ", 请求用户ID: " + userId);
                return false;
            }
            
            // 删除评论
            commentRepository.deleteById(commentId);
            System.out.println("评论删除成功，ID: " + commentId);
            return true;
        } catch (Exception e) {
            System.err.println("删除评论失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 