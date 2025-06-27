package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.UserSearchHistoryDTO;
import com.dealwithpapers.dealwithpapers.dto.UserViewHistoryDTO;
import com.dealwithpapers.dealwithpapers.service.UserHistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class UserHistoryController {

    private final UserHistoryService userHistoryService;

    // ===== 搜索历史相关接口 =====

    /**
     * 保存用户搜索记录
     * @param searchText 搜索文本
     * @param session HTTP会话
     * @return 保存的搜索记录
     */
    @PostMapping("/search")
    public ResponseEntity<?> saveSearchHistory(@RequestParam String searchText, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            UserSearchHistoryDTO savedHistory = userHistoryService.saveSearchHistory(userId, searchText);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedHistory);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户的搜索历史（分页）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param session HTTP会话
     * @return 分页的搜索历史记录
     */
    @GetMapping("/search")
    public ResponseEntity<?> getUserSearchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 限制每页大小最大为50
            size = Math.min(size, 50);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<UserSearchHistoryDTO> historyPage = userHistoryService.getUserSearchHistory(userId, pageable);
            return ResponseEntity.ok(historyPage);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户最近的搜索历史
     * @param limit 限制返回的记录数量
     * @param session HTTP会话
     * @return 最近的搜索历史记录列表
     */
    @GetMapping("/search/recent")
    public ResponseEntity<?> getRecentSearchHistory(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 限制最大返回记录数为50
            limit = Math.min(limit, 50);
            
            List<UserSearchHistoryDTO> recentHistory = userHistoryService.getRecentSearchHistory(userId, limit);
            return ResponseEntity.ok(recentHistory);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户最近的不重复搜索词
     * @param limit 限制返回的记录数量
     * @param session HTTP会话
     * @return 不重复的搜索词列表
     */
    @GetMapping("/search/terms")
    public ResponseEntity<?> getDistinctSearchTerms(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 限制最大返回记录数为50
            limit = Math.min(limit, 50);
            
            List<String> searchTerms = userHistoryService.getDistinctSearchTerms(userId, limit);
            return ResponseEntity.ok(searchTerms);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 清空用户的所有搜索历史
     * @param session HTTP会话
     * @return 操作结果
     */
    @DeleteMapping("/search/clear")
    public ResponseEntity<?> clearUserSearchHistory(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            userHistoryService.clearUserSearchHistory(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "搜索历史清空成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除指定的搜索记录
     * @param id 搜索记录ID
     * @param session HTTP会话
     * @return 操作结果
     */
    @DeleteMapping("/search/{id}")
    public ResponseEntity<?> deleteSearchHistory(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            userHistoryService.deleteSearchHistory(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "搜索记录删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ===== 浏览历史相关接口 =====

    /**
     * 保存用户浏览记录
     * @param paperId 论文ID
     * @param session HTTP会话
     * @return 保存的浏览记录
     */
    @PostMapping("/view")
    public ResponseEntity<?> saveViewHistory(@RequestParam String paperId, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            UserViewHistoryDTO savedHistory = userHistoryService.saveViewHistory(userId, paperId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedHistory);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户的浏览历史（分页）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param session HTTP会话
     * @return 分页的浏览历史记录
     */
    @GetMapping("/view")
    public ResponseEntity<?> getUserViewHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 限制每页大小最大为50
            size = Math.min(size, 50);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<UserViewHistoryDTO> historyPage = userHistoryService.getUserViewHistory(userId, pageable);
            return ResponseEntity.ok(historyPage);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户最近的浏览历史
     * @param limit 限制返回的记录数量
     * @param session HTTP会话
     * @return 最近的浏览历史记录列表
     */
    @GetMapping("/view/recent")
    public ResponseEntity<?> getRecentViewHistory(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 限制最大返回记录数为50
            limit = Math.min(limit, 50);
            
            List<UserViewHistoryDTO> recentHistory = userHistoryService.getRecentViewHistory(userId, limit);
            return ResponseEntity.ok(recentHistory);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取论文的浏览次数
     * @param paperId 论文ID
     * @return 浏览次数
     */
    @GetMapping("/view/count/{paperId}")
    public ResponseEntity<?> getPaperViewCount(@PathVariable String paperId) {
        try {
            long viewCount = userHistoryService.getPaperViewCount(paperId);
            Map<String, Long> response = new HashMap<>();
            response.put("viewCount", viewCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 清空用户的所有浏览历史
     * @param session HTTP会话
     * @return 操作结果
     */
    @DeleteMapping("/view/clear")
    public ResponseEntity<?> clearUserViewHistory(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            userHistoryService.clearUserViewHistory(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "浏览历史清空成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除指定的浏览记录
     * @param id 浏览记录ID
     * @param session HTTP会话
     * @return 操作结果
     */
    @DeleteMapping("/view/{id}")
    public ResponseEntity<?> deleteViewHistory(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            userHistoryService.deleteViewHistory(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "浏览记录删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}