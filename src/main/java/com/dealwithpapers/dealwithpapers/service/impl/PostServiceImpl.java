package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaperRepository paperRepository;

    @Override
    public PostDTO createPost(PostDTO postDTO) {
        Post post = new Post();
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        post.setType(postDTO.getType());
        post.setCategory(postDTO.getCategory());
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        post.setStatus(1);
        // 关联作者
        Optional<User> userOpt = userRepository.findById(postDTO.getAuthorId());
        userOpt.ifPresent(post::setAuthor);
        // 关联论文
        if (postDTO.getPaperId() != null) {
            Optional<Paper> paperOpt = paperRepository.findById(postDTO.getPaperId());
            paperOpt.ifPresent(post::setPaper);
        }
        Post saved = postRepository.save(post);
        return toDTO(saved);
    }

    @Override
    public List<PostDTO> searchPostsByTitle(String title) {
        return postRepository.findByTitleContaining(title).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PostDTO getPostById(Long id) {
        return postRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<PostDTO> searchByTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return postRepository.findAll().stream()
                .filter(post -> post.getStatus() == 1)
                .map(this::toDTO).collect(Collectors.toList());
        }
        return new java.util.ArrayList<>(new java.util.HashSet<>(postRepository.searchByTerm(searchTerm.trim())))
            .stream().filter(post -> post.getStatus() == 1)
            .map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> searchPosts(String keyword, String author, String type, String category, Integer page, Integer size) {
        String searchTerm = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() :
                            (author != null && !author.trim().isEmpty()) ? author.trim() :
                            (type != null && !type.trim().isEmpty()) ? type.trim() :
                            (category != null && !category.trim().isEmpty()) ? category.trim() : "";
        return searchByTerm(searchTerm);
    }

    @Override
    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setStatus(0);
        postRepository.save(post);
    }

    private PostDTO toDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setType(post.getType());
        dto.setCategory(post.getCategory());
        dto.setStatus(post.getStatus());
        if (post.getAuthor() != null) {
            dto.setAuthorId(post.getAuthor().getId());
            dto.setAuthorName(post.getAuthor().getUsername());
        }
        if (post.getPaper() != null) {
            dto.setPaperId(post.getPaper().getId());
            dto.setPaperTitle(post.getPaper().getTitle());
        }
        dto.setCreateTime(post.getCreateTime());
        dto.setUpdateTime(post.getUpdateTime());
        return dto;
    }
} 