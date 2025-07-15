package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PostDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.CommentService;
import com.dealwithpapers.dealwithpapers.service.PostService;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.dealwithpapers.dealwithpapers.dto.PaperDTO;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;

    @Autowired
    private PostLikeService postLikeService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户
     * @return 当前用户ID
     */
    private User getCurrentUser() {
        return AuthUtils.getCurrentUser(userRepository);
    }

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
            result.put("authorId", post.getAuthorId());
            result.put("authorAvatar", post.getAuthorAvatar());
            result.put("authorTitle", ""); // 可扩展
            result.put("authorBio", ""); // 可扩展
            result.put("likes", postLikeService.countLikes(post.getId()));
            result.put("dislikes", postLikeService.countDislikes(post.getId()));
            result.put("comments", commentService.countCommentsByPostId(post.getId()));
            result.put("postTags", post.getPostTags());
            result.put("views", post.getViews()); // 返回真实浏览量
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
            result.put("authorId", post.getAuthorId());
            result.put("authorAvatar", post.getAuthorAvatar());
            result.put("authorTitle", ""); // 可扩展
            result.put("authorBio", ""); // 可扩展
            result.put("likes", postLikeService.countLikes(post.getId()));
            result.put("dislikes", postLikeService.countDislikes(post.getId()));
            result.put("comments", commentService.countCommentsByPostId(post.getId()));
            result.put("postTags", post.getPostTags());
            result.put("views", post.getViews()); // 返回真实浏览量
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
        result.put("authorId", post.getAuthorId());
        result.put("authorAvatar", post.getAuthorAvatar());
        result.put("authorTitle", ""); // 可扩展
        result.put("authorBio", ""); // 可扩展
        result.put("likes", postLikeService.countLikes(id));
        result.put("dislikes", postLikeService.countDislikes(id));
        result.put("comments", commentService.countCommentsByPostId(id));
        result.put("postTags", post.getPostTags());
        result.put("views", post.getViews()); // 返回真实浏览量
        
        // 添加主要论文信息
        if (post.getPaperId() != null) {
            Map<String, Object> mainPaper = new HashMap<>();
            mainPaper.put("id", post.getPaperId());
            mainPaper.put("title", post.getPaperTitle());
            result.put("mainPaper", mainPaper);
        }
        
        // 添加相关论文信息
        List<Map<String, Object>> relatedPapers = new ArrayList<>();
        if (post.getRelatedPapers() != null && !post.getRelatedPapers().isEmpty()) {
            for (PaperDTO paper : post.getRelatedPapers()) {
                Map<String, Object> paperMap = new HashMap<>();
                paperMap.put("id", paper.getId());
                paperMap.put("title", paper.getTitle());
                paperMap.put("authors", paper.getAuthors());
                paperMap.put("abstract", paper.getAbstractText());
                paperMap.put("year", paper.getYear());
                paperMap.put("doi", paper.getDoi());
                paperMap.put("url", paper.getUrl());
                relatedPapers.add(paperMap);
            }
        }
        result.put("relatedPapers", relatedPapers);
        result.put("relatedPosts", new Object[]{}); // 暂无相关帖子
        result.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        return result;
    }
    
    // 更新帖子关联论文的API
    @PostMapping("/{id}/papers")
    public Map<String, Object> updatePostRelatedPapers(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 检查当前用户是否是帖子作者
            User currentUser = getCurrentUser();
            PostDTO post = postService.getPostById(id);
            if (post == null) {
                response.put("success", false);
                response.put("message", "帖子不存在");
                return response;
            }
            
            if (!post.getAuthorId().equals(currentUser.getId())) {
                response.put("success", false);
                response.put("message", "只能修改自己发布的帖子");
                return response;
            }
            
            // 更新主要论文
            Long mainPaperId = request.get("paperId") != null ? Long.valueOf(request.get("paperId").toString()) : null;
            if (mainPaperId != null) {
                post.setPaperId(mainPaperId);
            }
            
            // 更新关联论文
            if (request.get("relatedPaperIds") != null) {
                List<?> paperIds = (List<?>) request.get("relatedPaperIds");
                Set<Long> relatedPaperIds = new HashSet<>();
                for (Object paperItemId : paperIds) {
                    relatedPaperIds.add(Long.valueOf(paperItemId.toString()));
                }
                post.setRelatedPaperIds(relatedPaperIds);
            }
            
            // 保存更新
            PostDTO updatedPost = postService.createPost(post);
            
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("post", updatedPost);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
        }
        return response;
    }
    
    // 获取与指定论文相关的帖子
    @GetMapping("/byPaper/{paperId}")
    public List<Map<String, Object>> getPostsByPaper(@PathVariable Long paperId) {
        try {
            List<PostDTO> posts = postService.searchPostsByPaper(paperId);
            return posts.stream().map(post -> {
                Map<String, Object> result = new HashMap<>();
                result.put("id", post.getId());
                result.put("title", post.getTitle());
                result.put("content", post.getContent());
                result.put("category", post.getCategory());
                result.put("type", post.getType());
                result.put("author", post.getAuthorName());
                result.put("authorId", post.getAuthorId());
                result.put("authorAvatar", post.getAuthorAvatar());
                result.put("likes", postLikeService.countLikes(post.getId()));
                result.put("dislikes", postLikeService.countDislikes(post.getId()));
                result.put("comments", 0); // 暂无评论统计
                result.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return result;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @PostMapping("/{id}/view")
    public Map<String, Object> incrementViews(@PathVariable Long id) {
        postService.incrementViews(id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("views", postService.getViews(id));
        return res;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletePost(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 检查当前用户是否是帖子作者
            User currentUser = getCurrentUser();
            PostDTO post = postService.getPostById(id);
            if (post == null) {
                response.put("success", false);
                response.put("message", "帖子不存在");
                return response;
            }
            
            if (!post.getAuthorId().equals(currentUser.getId())) {
                response.put("success", false);
                response.put("message", "只能删除自己发布的帖子");
                return response;
            }
            
            postService.deletePost(id);
            response.put("success", true);
            response.put("message", "帖子删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/byTag")
    public List<PostDTO> getPostsByPostTag(@RequestParam String postTag) {
        return postService.searchPostsByTag(postTag);
    }
    
    // 获取当前用户发布的帖子
    @GetMapping("/user/published")
    public Map<String, Object> getUserPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            List<PostDTO> posts = postService.searchPosts(null, currentUser.getUsername(), null, null, null, null);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = posts.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("authorId", post.getAuthorId());
                item.put("authorAvatar", post.getAuthorAvatar());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", commentService.countCommentsByPostId(post.getId()));
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return item;
            }).toList();
            
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取发布帖子失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
        }
        return response;
    }
    
    // 获取当前用户点赞的帖子
    @GetMapping("/user/liked")
    public Map<String, Object> getUserLikedPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            List<PostDTO> posts = postLikeService.getUserLikedPosts(currentUser.getId());
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = posts.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("authorId", post.getAuthorId());
                item.put("authorAvatar", post.getAuthorAvatar());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", commentService.countCommentsByPostId(post.getId()));
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return item;
            }).toList();
            
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取点赞帖子失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
        }
        return response;
    }
    
    // 获取当前用户点踩的帖子
    @GetMapping("/user/disliked")
    public Map<String, Object> getUserDislikedPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            List<PostDTO> posts = postLikeService.getUserDislikedPosts(currentUser.getId());
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = posts.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("authorId", post.getAuthorId());
                item.put("authorAvatar", post.getAuthorAvatar());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", commentService.countCommentsByPostId(post.getId()));
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return item;
            }).toList();
            
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取点踩帖子失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
        }
        return response;
    }
    
    // 获取当前用户评论的帖子
    @GetMapping("/user/commented")
    public Map<String, Object> getUserCommentedPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            
            // 使用评论服务获取用户评论过的帖子
            List<PostDTO> posts = commentService.getUserCommentedPosts(currentUser.getId());
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = posts.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("authorId", post.getAuthorId());
                item.put("authorAvatar", post.getAuthorAvatar());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", commentService.countCommentsByPostId(post.getId()));
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return item;
            }).toList();
            
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取评论帖子失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
        }
        return response;
    }

    /**
     * 获取指定用户发布的帖子
     * @param userId 用户ID
     * @return 帖子列表
     */
    @GetMapping("/user/{userId}/posts")
    public Map<String, Object> getUserPostsByUserId(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<PostDTO> posts = postService.searchPosts(null, null, null, null, userId, null);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = posts.stream().map(post -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("title", post.getTitle());
                item.put("content", post.getContent());
                item.put("category", post.getCategory());
                item.put("type", post.getType());
                item.put("author", post.getAuthorName());
                item.put("authorId", post.getAuthorId());
                item.put("authorAvatar", post.getAuthorAvatar());
                item.put("likes", postLikeService.countLikes(post.getId()));
                item.put("dislikes", postLikeService.countDislikes(post.getId()));
                item.put("comments", commentService.countCommentsByPostId(post.getId()));
                item.put("time", post.getCreateTime() != null ? post.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                return item;
            }).toList();
            
            response.put("success", true);
            response.put("data", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户帖子失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
        }
        return response;
    }
} 