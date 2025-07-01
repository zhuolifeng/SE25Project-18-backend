package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.service.PaperService;
import com.dealwithpapers.dealwithpapers.service.UserFavoriteService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/favorites")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService userFavoriteService;
    
    @Autowired
    private PaperService paperService;
    
    // 会话中用户ID的键名，与UserServiceImpl中保持一致
    private static final String USER_SESSION_KEY = "currentUser";
    
    /**
     * 收藏论文
     * 支持从URL参数或请求体中获取paperId
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addFavorite(
            @RequestParam(required = false) String paperId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session,
            HttpServletRequest request) {
        
        System.out.println("=================== 收藏论文请求开始 ===================");
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("请求路径: " + request.getRequestURI());
        System.out.println("请求完整URL: " + request.getRequestURL());
        System.out.println("客户端IP: " + request.getRemoteAddr());
        System.out.println("ContentType: " + request.getContentType());
        
        // 打印所有请求头
        System.out.println("请求头信息:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("  " + headerName + ": " + request.getHeader(headerName));
        }
        
        // 打印所有Cookie
        System.out.println("Cookie信息:");
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                System.out.println("  " + cookie.getName() + ": " + cookie.getValue());
            }
        } else {
            System.out.println("  无Cookie信息");
        }
        
        System.out.println("URL参数 paperId: " + paperId);
        System.out.println("请求体: " + body);
        
        // 打印所有session属性
        System.out.println("Session属性:");
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            System.out.println("  " + name + ": " + session.getAttribute(name));
        }
        
        Map<String, Object> response = new HashMap<>();
        
        // 从session获取当前用户ID，使用与UserServiceImpl一致的键
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        System.out.println("从Session获取的userId: " + userId);
        
        if (userId == null) {
            System.out.println("用户未登录，无法收藏论文");
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.ok(response);
        }
        
        // 尝试从请求体或URL参数中获取论文ID
        Long paperIdLong = null;
        
        // 先尝试从body中获取paperId
        if (body != null && body.containsKey("paperId")) {
            Object idObj = body.get("paperId");
            System.out.println("从请求体中获取paperId: " + idObj);
            
            if (idObj instanceof Number) {
                paperIdLong = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    paperIdLong = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    System.out.println("请求体中的论文ID格式错误: " + e.getMessage());
                }
            }
        }
        
        // 如果从请求体没有获取到有效的ID，尝试从URL参数获取
        if (paperIdLong == null && paperId != null && !paperId.trim().isEmpty()) {
            try {
                paperIdLong = Long.parseLong(paperId);
                System.out.println("从URL参数获取paperId: " + paperIdLong);
            } catch (NumberFormatException e) {
                System.out.println("URL参数中的论文ID格式错误: " + e.getMessage());
            }
        }
        
        if (paperIdLong == null) {
            System.out.println("无法获取有效的论文ID");
            response.put("success", false);
            response.put("message", "论文ID不能为空或格式不正确");
            return ResponseEntity.ok(response);
        }
        
        System.out.println("最终确定的论文ID: " + paperIdLong);
        
        boolean success = userFavoriteService.addFavorite(userId, paperIdLong);
        System.out.println("收藏结果: " + (success ? "成功" : "失败"));
        
        response.put("success", success);
        if (success) {
            response.put("message", "收藏成功");
        } else {
            response.put("message", "收藏失败，可能已经收藏过或论文不存在");
        }
        
        System.out.println("响应内容: " + response);
        System.out.println("=================== 收藏论文请求结束 ===================");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 取消收藏
     * 支持从URL参数或请求体中获取paperId
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @RequestParam(required = false) String paperId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        
        System.out.println("=================== 取消收藏请求开始 ===================");
        System.out.println("URL参数 paperId: " + paperId);
        System.out.println("请求体: " + body);
        
        Map<String, Object> response = new HashMap<>();
        
        // 从session获取当前用户ID，使用与UserServiceImpl一致的键
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        System.out.println("从Session获取的userId: " + userId);
        
        if (userId == null) {
            System.out.println("用户未登录，无法取消收藏");
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.ok(response);
        }
        
        // 尝试从请求体或URL参数中获取论文ID
        Long paperIdLong = null;
        
        // 先尝试从body中获取paperId
        if (body != null && body.containsKey("paperId")) {
            Object idObj = body.get("paperId");
            System.out.println("从请求体中获取paperId: " + idObj);
            
            if (idObj instanceof Number) {
                paperIdLong = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    paperIdLong = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    System.out.println("请求体中的论文ID格式错误: " + e.getMessage());
                }
            }
        }
        
        // 如果从请求体没有获取到有效的ID，尝试从URL参数获取
        if (paperIdLong == null && paperId != null && !paperId.trim().isEmpty()) {
            try {
                paperIdLong = Long.parseLong(paperId);
                System.out.println("从URL参数获取paperId: " + paperIdLong);
            } catch (NumberFormatException e) {
                System.out.println("URL参数中的论文ID格式错误: " + e.getMessage());
            }
        }
        
        if (paperIdLong == null) {
            System.out.println("无法获取有效的论文ID");
            response.put("success", false);
            response.put("message", "论文ID不能为空或格式不正确");
            return ResponseEntity.ok(response);
        }
        
        System.out.println("最终确定的论文ID: " + paperIdLong);
        
        boolean success = userFavoriteService.removeFavorite(userId, paperIdLong);
        System.out.println("取消收藏结果: " + (success ? "成功" : "失败"));
        
        response.put("success", success);
        if (success) {
            response.put("message", "取消收藏成功");
        } else {
            response.put("message", "取消收藏失败，该论文未被收藏");
        }
        
        System.out.println("响应内容: " + response);
        System.out.println("=================== 取消收藏请求结束 ===================");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户收藏列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 从session获取当前用户ID，使用与UserServiceImpl一致的键
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.ok(response);
        }
        
        // 创建分页请求，按收藏时间降序排序
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "collectTime"));
        
        // 获取收藏列表
        Page<PaperDTO> favoritesPage = userFavoriteService.getUserFavorites(userId, pageRequest);
        
        response.put("success", true);
        response.put("data", favoritesPage.getContent());
        response.put("currentPage", favoritesPage.getNumber());
        response.put("totalItems", favoritesPage.getTotalElements());
        response.put("totalPages", favoritesPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 检查论文是否被用户收藏
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkIsFavorite(
            @RequestParam String paperId,
            HttpSession session,
            HttpServletRequest request) {
        
        System.out.println("=================== 检查收藏状态请求开始 ===================");
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("请求路径: " + request.getRequestURI());
        System.out.println("请求完整URL: " + request.getRequestURL() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
        System.out.println("URL参数 paperId: " + paperId);
        
        // 打印所有session属性
        System.out.println("Session属性:");
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            System.out.println("  " + name + ": " + session.getAttribute(name));
        }
        
        Map<String, Object> response = new HashMap<>();
        
        // 从session获取当前用户ID
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        System.out.println("从Session获取的userId: " + userId);
        
        if (userId == null) {
            System.out.println("用户未登录，无法检查收藏状态");
            response.put("success", false);
            response.put("message", "用户未登录");
            response.put("favorite", false);
            return ResponseEntity.ok(response);
        }
        
        // 尝试解析论文ID
        Long paperIdLong = null;
        try {
            paperIdLong = Long.parseLong(paperId);
            System.out.println("解析后的论文ID: " + paperIdLong);
        } catch (NumberFormatException e) {
            System.out.println("论文ID格式错误: " + e.getMessage());
            response.put("success", false);
            response.put("message", "论文ID格式不正确");
            response.put("favorite", false);
            return ResponseEntity.ok(response);
        }
        
        boolean isFavorite = userFavoriteService.checkIsFavorite(userId, paperIdLong);
        System.out.println("检查收藏结果: 用户ID=" + userId + ", 论文ID=" + paperIdLong + ", 是否收藏=" + isFavorite);
        
        response.put("success", true);
        response.put("favorite", isFavorite);
        
        System.out.println("响应内容: " + response);
        System.out.println("=================== 检查收藏状态请求结束 ===================");
        
        return ResponseEntity.ok(response);
    }
} 