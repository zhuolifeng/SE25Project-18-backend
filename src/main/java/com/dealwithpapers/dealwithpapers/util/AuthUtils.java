package com.dealwithpapers.dealwithpapers.util;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 认证工具类，提供获取当前用户的统一方法
 */
public class AuthUtils {
    
    /**
     * 获取当前登录用户
     * @param userRepository 用户仓库
     * @return 当前用户对象
     * @throws IllegalStateException 如果用户未认证或无法获取用户信息
     */
    public static User getCurrentUser(UserRepository userRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查认证是否存在且不是匿名认证
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            System.out.println("AuthUtils - 用户未认证或是匿名用户");
            throw new IllegalStateException("用户未认证或是匿名用户");
        }
        
        // 添加调试信息
        Object principal = authentication.getPrincipal();
        System.out.println("AuthUtils - Principal类型: " + (principal != null ? principal.getClass().getName() : "null"));
        
        // 增强类型检查和转换逻辑
        if (principal instanceof User) {
            User user = (User) principal;
            System.out.println("AuthUtils - Principal是User对象: " + user.getUsername() + ", ID: " + user.getId());
            return user;
        } else if (principal instanceof String) {
            // 用户名字符串，需要从数据库获取用户对象
            String username = (String) principal;
            System.out.println("AuthUtils - Principal是字符串: " + username);
            
            // 跳过匿名用户
            if ("anonymousUser".equals(username)) {
                System.out.println("AuthUtils - 匿名用户，无法获取用户信息");
                throw new IllegalStateException("匿名用户，请先登录");
            }
            
            try {
                // 尝试通过用户名查询用户
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("找不到用户: " + username));
                System.out.println("AuthUtils - 通过用户名找到用户: " + user.getUsername() + ", ID: " + user.getId());
                return user;
            } catch (Exception e) {
                System.err.println("AuthUtils - 查询用户失败: " + e.getMessage());
                e.printStackTrace();
                throw new IllegalStateException("无法获取当前用户信息: " + e.getMessage());
            }
        } else {
            System.err.println("AuthUtils - 未知的Principal类型: " + (principal != null ? principal.getClass().getName() : "null"));
            throw new IllegalStateException("未知的用户认证方式");
        }
    }
    
    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !(authentication instanceof AnonymousAuthenticationToken);
    }
} 