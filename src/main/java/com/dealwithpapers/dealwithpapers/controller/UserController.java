package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.UserUpdateDTO;
import com.dealwithpapers.dealwithpapers.dto.PasswordUpdateDTO;
import com.dealwithpapers.dealwithpapers.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Enumeration;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
            response.put("message", "退出登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
} 