package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping
    public PostDTO createPost(@RequestBody PostDTO postDTO) {
        return postService.createPost(postDTO);
    }

    @PostMapping("/search")
    public List<PostDTO> searchPosts(@RequestBody(required = false) PostDTO postDTO) {
        String keyword = postDTO != null ? postDTO.getTitle() : null;
        String author = postDTO != null ? postDTO.getAuthorName() : null;
        String type = postDTO != null ? postDTO.getType() : null;
        String category = postDTO != null ? postDTO.getCategory() : null;
        // 暂不处理分页
        return postService.searchPosts(keyword, author, type, category, null, null);
    }

    @GetMapping("/search")
    public List<PostDTO> searchPostsByGet(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String category
    ) {
        return postService.searchPosts(keyword, author, type, category, null, null);
    }

    @GetMapping("/{id}")
    public PostDTO getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }
} 