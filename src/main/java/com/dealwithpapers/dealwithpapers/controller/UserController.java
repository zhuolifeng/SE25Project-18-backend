package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.UserUpdateDTO;
import com.dealwithpapers.dealwithpapers.dto.PasswordUpdateDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.repository.UserFollowRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserFavoriteRepository;
import com.dealwithpapers.dealwithpapers.service.UserService;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserFollowRepository userFollowRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterDTO registerDTO) {
        try {
            UserResponseDTO userResponseDTO = userService.register(registerDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO loginDTO) {
        try {
            UserResponseDTO userResponseDTO = userService.login(loginDTO);
            return ResponseEntity.ok(userResponseDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        try {
            UserResponseDTO userResponseDTO = userService.getCurrentUser();
            return ResponseEntity.ok(userResponseDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            userService.logout();
            Map<String, String> response = new HashMap<>();
            response.put("message", "已成功退出登录");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserInfo(@RequestBody UserUpdateDTO updateDTO) {
        try {
            UserResponseDTO userResponseDTO = userService.updateUserInfo(updateDTO);
            return ResponseEntity.ok(userResponseDTO);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateDTO passwordUpdateDTO) {
        try {
            userService.updatePassword(passwordUpdateDTO);
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码修改成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            // 获取当前登录用户
            User user = AuthUtils.getCurrentUser(userRepository);
            // 保存文件到本地
            String uploadDir = "uploads/avatars/";
            Files.createDirectories(Paths.get(uploadDir));
            String filename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            Path filePath = Paths.get(uploadDir, filename);
            file.transferTo(filePath);
            // 更新用户头像URL
            String avatarUrl = "/" + uploadDir + filename;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            // 返回新头像URL
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/session-test")
    public ResponseEntity<?> testSession(HttpSession session) {
        System.out.println("=================== 测试会话状态 ===================");
        
        Map<String, Object> response = new HashMap<>();
        
        // 打印会话信息
        System.out.println("会话ID: " + session.getId());
        System.out.println("会话创建时间: " + new Date(session.getCreationTime()));
        System.out.println("会话是否为新会话: " + session.isNew());
        System.out.println("会话超时时间: " + session.getMaxInactiveInterval() + "秒");
        
        // 获取所有会话属性
        System.out.println("会话属性列表:");
        Enumeration<String> attributeNames = session.getAttributeNames();
        Map<String, Object> sessionAttributes = new HashMap<>();
        
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            Object value = session.getAttribute(name);
            System.out.println("  " + name + ": " + value);
            sessionAttributes.put(name, value == null ? "null" : value.toString());
        }
        
        // 从会话获取当前用户ID
        Long userId = (Long) session.getAttribute("currentUser");
        boolean isLoggedIn = userId != null;
        
        // 构建响应
        response.put("sessionId", session.getId());
        response.put("isNewSession", session.isNew());
        response.put("creationTime", new Date(session.getCreationTime()).toString());
        response.put("lastAccessedTime", new Date(session.getLastAccessedTime()).toString());
        response.put("maxInactiveInterval", session.getMaxInactiveInterval());
        response.put("attributes", sessionAttributes);
        response.put("isLoggedIn", isLoggedIn);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户公开资料
     * @param userId 用户ID
     * @return 用户公开资料
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserPublicProfile(@PathVariable Long userId) {
        try {
            Map<String, Object> profile = userService.getUserPublicProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 关注用户
     * @param userId 要关注的用户ID
     * @return 操作结果
     */
    @PostMapping("/follow/{userId}")
    public ResponseEntity<?> followUser(@PathVariable Long userId) {
        try {
            Map<String, Object> result = userService.followUser(userId);
            if ((boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 取消关注用户
     * @param userId 要取消关注的用户ID
     * @return 操作结果
     */
    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId) {
        try {
            Map<String, Object> result = userService.unfollowUser(userId);
            if ((boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 获取用户关注的用户列表
     * @param userId 用户ID
     * @return 关注的用户列表
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<?> getFollowingList(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> followingList = userService.getFollowingList(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", followingList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("data", List.of());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 获取用户的粉丝列表
     * @param userId 用户ID
     * @return 粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<?> getFollowersList(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> followersList = userService.getFollowersList(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", followersList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("data", List.of());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 检查当前用户是否关注了指定用户
     * @param userId 要检查的用户ID
     * @return 是否已关注
     */
    @GetMapping("/is-following/{userId}")
    public ResponseEntity<?> isFollowing(@PathVariable Long userId) {
        try {
            boolean isFollowing = userService.isFollowing(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isFollowing", isFollowing);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("isFollowing", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取当前用户的统计数据
     * @return 用户统计数据
     */
    @GetMapping("/current/stats")
    public ResponseEntity<?> getCurrentUserStats() {
        try {
            User currentUser = AuthUtils.getCurrentUser(userRepository);
            if (currentUser == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            Map<String, Object> stats = getUserStatsData(currentUser.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 获取指定用户的统计数据
     * @param userId 用户ID
     * @return 用户统计数据
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable Long userId) {
        try {
            if (!userRepository.existsById(userId)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            Map<String, Object> stats = getUserStatsData(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 获取用户统计数据的共用方法
     * @param userId 用户ID
     * @return 统计数据Map
     */
    private Map<String, Object> getUserStatsData(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 获取统计数据
        int postCount = postRepository.countByAuthorId(userId);
        long followersCount = userFollowRepository.countByFollowing(user);
        long followingCount = userFollowRepository.countByFollower(user);
        long favoriteCount = userFavoriteRepository.countByUserId(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", postCount);
        stats.put("followerCount", followersCount);
        stats.put("followingCount", followingCount);
        stats.put("favoriteCount", favoriteCount);
        
        // 如果当前有登录用户，检查是否已关注该用户
        try {
            User currentUser = AuthUtils.getCurrentUser(userRepository);
            if (currentUser != null && !currentUser.getId().equals(userId)) {
                boolean isFollowing = userService.isFollowing(userId);
                stats.put("isFollowing", isFollowing);
            }
        } catch (Exception e) {
            // 用户未登录，不处理
            stats.put("isFollowing", false);
        }
        
        return stats;
    }
} 