package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.CommentDTO;
import com.dealwithpapers.dealwithpapers.entity.Comment;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.CommentRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private CommentDTO toDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setPostId(comment.getPost().getId());
        dto.setUserId(comment.getUser().getId());
        dto.setUserName(comment.getUser().getUsername());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setCreateTime(comment.getCreateTime());
        return dto;
    }
} 