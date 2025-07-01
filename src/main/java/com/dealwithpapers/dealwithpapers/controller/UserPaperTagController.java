package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.UserPaperTagDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.service.UserPaperTagService;
import com.dealwithpapers.dealwithpapers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class UserPaperTagController {

    private final UserPaperTagService userPaperTagService;
    private final UserService userService;

    @Autowired
    public UserPaperTagController(UserPaperTagService userPaperTagService, UserService userService) {
        this.userPaperTagService = userPaperTagService;
        this.userService = userService;
    }

    /**
     * 添加用户自定义标签
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addTag(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> requestBody) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 从请求体中获取参数
        Long paperId = Long.valueOf(requestBody.get("paperId").toString());
        String tagName = (String) requestBody.get("tagName");
        String tagColor = requestBody.get("tagColor") != null ? 
                (String) requestBody.get("tagColor") : null;
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 如果没有提供颜色，使用默认颜色
            if (tagColor == null || tagColor.trim().isEmpty()) {
                tagColor = generateTagColor(tagName);
            }
            
            // 添加标签
            UserPaperTagDTO tagDTO = userPaperTagService.addTag(
                    currentUser.getId(), paperId, tagName, tagColor);
            
            response.put("success", true);
            response.put("data", tagDTO);
            response.put("message", "标签添加成功");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "添加标签失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除用户标签
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeTag(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> requestBody) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 从请求体中获取参数
        Long paperId = Long.valueOf(requestBody.get("paperId").toString());
        String tagName = (String) requestBody.get("tagName");
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 删除标签
            boolean removed = userPaperTagService.removeTag(
                    currentUser.getId(), paperId, tagName);
            
            if (removed) {
                response.put("success", true);
                response.put("message", "标签已删除");
            } else {
                response.put("success", false);
                response.put("message", "标签不存在或删除失败");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "删除标签失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取特定论文的所有标签
     */
    @GetMapping("/paper/{paperId}")
    public ResponseEntity<Map<String, Object>> getTagsByPaper(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("paperId") Long paperId) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 获取论文的标签
            List<UserPaperTagDTO> tags = userPaperTagService.getTagsByPaper(
                    currentUser.getId(), paperId);
            
            response.put("success", true);
            response.put("data", tags);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "获取标签失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户的所有标签和关联的论文
     */
    @GetMapping("/user/all")
    public ResponseEntity<Map<String, Object>> getAllUserTags(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 获取用户所有标签
            Map<String, List<UserPaperTagDTO>> tags = userPaperTagService.getAllUserTags(
                    currentUser.getId());
            
            response.put("success", true);
            response.put("data", tags);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "获取标签失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取特定标签关联的所有论文
     */
    @GetMapping("/name/{tagName}")
    public ResponseEntity<Map<String, Object>> getPapersByTag(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("tagName") String tagName) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 获取标签关联的论文
            List<UserPaperTagDTO> papers = userPaperTagService.getPapersByTag(
                    currentUser.getId(), tagName);
            
            response.put("success", true);
            response.put("data", papers);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "获取论文失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新标签颜色
     */
    @PutMapping("/update-color")
    public ResponseEntity<Map<String, Object>> updateTagColor(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> requestBody) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 从请求体中获取参数
        Long paperId = Long.valueOf(requestBody.get("paperId").toString());
        String tagName = (String) requestBody.get("tagName");
        String newColor = (String) requestBody.get("newColor");
        
        // 验证用户登录
        UserResponseDTO currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            response.put("success", false);
            response.put("notLoggedIn", true);
            response.put("message", "请先登录");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 更新标签颜色
            UserPaperTagDTO tagDTO = userPaperTagService.updateTagColor(
                    currentUser.getId(), paperId, tagName, newColor);
            
            response.put("success", true);
            response.put("data", tagDTO);
            response.put("message", "标签颜色已更新");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "更新标签颜色失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据标签名生成颜色
     */
    private String generateTagColor(String tagName) {
        // 默认颜色列表
        String[] colors = {
            "#f50", "#2db7f5", "#87d068", "#108ee9", "#722ed1",
            "#eb2f96", "#faad14", "#a0d911", "#52c41a", "#13c2c2"
        };
        
        // 基于标签名哈希生成颜色索引
        int index = Math.abs(tagName.hashCode() % colors.length);
        return colors[index];
    }
} 