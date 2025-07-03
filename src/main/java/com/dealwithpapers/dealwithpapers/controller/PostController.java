package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.service.PostService;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;

    @Autowired
    private PostLikeService postLikeService;

    @PostMapping
    public PostDTO createPost(@RequestBody PostDTO postDTO) {
        return postService.createPost(postDTO);
    }

    @PostMapping("/search")
    public List<Map<String, Object>> searchPosts(@RequestBody(required = false) PostDTO postDTO) {
        String keyword = postDTO != null ? postDTO.getTitle() : null;
        String author = postDTO != null ? postDTO.getAuthorName() : null;
        String type = postDTO != null ? postDTO.getType() : null;
        String category = postDTO != null ? postDTO.getCategory() : null;
        List<PostDTO> posts = postService.searchPosts(keyword, author, type, category, null, null);
        return posts.stream().map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", post.getId());
            result.put("title", post.getTitle());
            result.put("content", post.getContent());
            result.put("category", post.getCategory());
            result.put("type", post.getType());
            result.put("author", post.getAuthorName());
            result.put("authorAvatar", ""); // 可扩展
            result.put("authorTitle", ""); // 可扩展
            result.put("authorBio", ""); // 可扩展
            result.put("likes", postLikeService.countLikes(post.getId()));
            result.put("dislikes", postLikeService.countDislikes(post.getId()));
            result.put("comments", 0); // 暂无评论统计
            result.put("postTags", post.getPostTags());
            result.put("views", 0); // 暂无浏览量
            result.put("relatedPapers", new Object[]{}); // 暂无相关论文
            result.put("relatedPosts", new Object[]{}); // 暂无相关帖子
            result.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            return result;
        }).toList();
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchPostsByGet(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String category
    ) {
        List<PostDTO> posts = postService.searchPosts(keyword, author, type, category, null, null);
        return posts.stream().map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", post.getId());
            result.put("title", post.getTitle());
            result.put("content", post.getContent());
            result.put("category", post.getCategory());
            result.put("type", post.getType());
            result.put("author", post.getAuthorName());
            result.put("authorAvatar", ""); // 可扩展
            result.put("authorTitle", ""); // 可扩展
            result.put("authorBio", ""); // 可扩展
            result.put("likes", postLikeService.countLikes(post.getId()));
            result.put("dislikes", postLikeService.countDislikes(post.getId()));
            result.put("comments", 0); // 暂无评论统计
            result.put("postTags", post.getPostTags());
            result.put("views", 0); // 暂无浏览量
            result.put("relatedPapers", new Object[]{}); // 暂无相关论文
            result.put("relatedPosts", new Object[]{}); // 暂无相关帖子
            result.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            return result;
        }).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPostById(@PathVariable Long id) {
        PostDTO post = postService.getPostById(id);
        if (post == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("id", post.getId());
        result.put("title", post.getTitle());
        result.put("content", post.getContent());
        result.put("category", post.getCategory());
        result.put("type", post.getType());
        result.put("author", post.getAuthorName());
        result.put("authorAvatar", ""); // 可扩展
        result.put("authorTitle", ""); // 可扩展
        result.put("authorBio", ""); // 可扩展
        result.put("likes", postLikeService.countLikes(id));
        result.put("dislikes", postLikeService.countDislikes(id));
        result.put("comments", 0); // 暂无评论统计
        result.put("postTags", post.getPostTags());
        result.put("views", 0); // 暂无浏览量
        result.put("relatedPapers", new Object[]{}); // 暂无相关论文
        result.put("relatedPosts", new Object[]{}); // 暂无相关帖子
        result.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        return result;
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id);
    }

    @GetMapping("/byTag")
    public List<PostDTO> getPostsByPostTag(@RequestParam String postTag) {
        return postService.searchPostsByTag(postTag);
    }
} 