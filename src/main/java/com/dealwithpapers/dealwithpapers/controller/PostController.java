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

    @GetMapping("/search")
    public List<PostDTO> searchPostsByTitle(@RequestParam String title) {
        return postService.searchPostsByTitle(title);
    }

    @GetMapping("/{id}")
    public PostDTO getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }
} 