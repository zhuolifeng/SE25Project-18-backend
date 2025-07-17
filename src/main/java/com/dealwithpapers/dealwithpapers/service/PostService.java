package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import java.util.List;

public interface PostService {
    PostDTO createPost(PostDTO postDTO);
    List<PostDTO> searchPostsByTitle(String title);
    PostDTO getPostById(Long id);
    List<PostDTO> searchPosts(String keyword, String author, String type, String category, Long userId, Integer size);
    List<PostDTO> searchByTerm(String searchTerm);
    void deletePost(Long id);
    List<PostDTO> searchPostsByTag(String tagName);
    
    /**
     * 根据论文ID查找相关帖子
     * 查找主要论文或关联论文包含指定论文ID的所有帖子
     * @param paperId 论文ID
     * @return 相关帖子列表
     */
    List<PostDTO> searchPostsByPaper(Long paperId);
    int getViews(Long postId);
    void incrementViews(Long postId);
} 