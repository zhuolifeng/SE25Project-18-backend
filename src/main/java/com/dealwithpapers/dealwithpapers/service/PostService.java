package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import java.util.List;

public interface PostService {
    PostDTO createPost(PostDTO postDTO);
    List<PostDTO> searchPostsByTitle(String title);
    PostDTO getPostById(Long id);
} 