package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.UserUpdateDTO;
import com.dealwithpapers.dealwithpapers.dto.PasswordUpdateDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserPassword;
import com.dealwithpapers.dealwithpapers.repository.UserPasswordRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpSession httpSession;
    
    private static final String USER_SESSION_KEY = "currentUser";

    @Override
    @Transactional
    public UserResponseDTO register(UserRegisterDTO registerDTO) {
        // 验证用户名是否已存在
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证邮箱是否已存在
        if (registerDTO.getEmail() != null && !registerDTO.getEmail().isEmpty() && userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 验证两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("两次密码不一致");
        }
        
        // 验证密码长度
        if (registerDTO.getPassword().length() < 6) {
            throw new RuntimeException("密码长度至少为6位");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setRegisterTime(LocalDateTime.now());
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 创建并保存密码
        UserPassword userPassword = new UserPassword();
        userPassword.setUser(savedUser);
        userPassword.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userPasswordRepository.save(userPassword);
        
        // 返回用户信息
        return convertToResponseDTO(savedUser);
    }

    @Override
    public UserResponseDTO login(UserLoginDTO loginDTO) {
        System.out.println("=================== 用户登录开始 ===================");
        System.out.println("登录用户名: " + loginDTO.getUsername());
        
        try {
            // 打印当前会话信息
            System.out.println("登录前的会话信息:");
            printSessionAttributes();
            
        // 查找用户
        Optional<User> userOptional = userRepository.findByUsername(loginDTO.getUsername());
        if (userOptional.isEmpty()) {
                System.out.println("用户名不存在: " + loginDTO.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }
        
        User user = userOptional.get();
            System.out.println("找到用户: ID=" + user.getId() + ", 用户名=" + user.getUsername());
        
        // 获取用户密码
        Optional<UserPassword> userPasswordOptional = userPasswordRepository.findById(user.getId());
        if (userPasswordOptional.isEmpty()) {
                System.out.println("用户密码信息不存在，用户ID: " + user.getId());
            throw new RuntimeException("用户密码信息不存在");
        }
        
        // 验证密码
            boolean passwordMatches = passwordEncoder.matches(loginDTO.getPassword(), userPasswordOptional.get().getPassword());
            System.out.println("密码验证结果: " + (passwordMatches ? "正确" : "错误"));
            
            if (!passwordMatches) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 保存用户信息到session
        httpSession.setAttribute(USER_SESSION_KEY, user.getId());
            System.out.println("用户ID已保存到会话，键名: " + USER_SESSION_KEY + ", 值: " + user.getId());
            System.out.println("会话ID: " + httpSession.getId());
            
            try {
                // 设置会话超时时间为1小时
                httpSession.setMaxInactiveInterval(3600);
                
                // 检查会话是否有效
                System.out.println("会话是否有效: " + !httpSession.isNew());
                System.out.println("会话超时时间: " + httpSession.getMaxInactiveInterval() + "秒");
                
                // 尝试再次确认会话中的用户ID
                Long confirmedUserId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
                System.out.println("确认会话中的用户ID: " + confirmedUserId);
                
                if (confirmedUserId == null || !confirmedUserId.equals(user.getId())) {
                    System.err.println("警告：无法正确存储用户ID到会话中！");
                }
            } catch (Exception e) {
                System.err.println("会话操作异常: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 打印更新后的会话信息
            System.out.println("登录后的会话信息:");
            printSessionAttributes();
        
        // 返回用户信息
            UserResponseDTO responseDTO = convertToResponseDTO(user);
            System.out.println("登录成功，返回用户信息: " + responseDTO);
            return responseDTO;
        } catch (Exception e) {
            System.err.println("登录失败: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("=================== 用户登录结束 ===================");
        }
    }

    @Override
    public UserResponseDTO getCurrentUser() {
        System.out.println("=================== 获取当前用户开始 ===================");
        
        try {
            // 打印当前会话信息
            System.out.println("当前会话信息:");
            printSessionAttributes();
            
        // 从session获取当前用户ID
        Long userId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
            System.out.println("从会话获取的用户ID: " + userId);
            
        if (userId == null) {
                System.out.println("用户未登录");
            throw new RuntimeException("用户未登录");
        }
        
        // 查找用户
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
                System.out.println("用户不存在，ID: " + userId);
            throw new RuntimeException("用户不存在");
        }
            
            User user = userOptional.get();
            System.out.println("找到用户: ID=" + user.getId() + ", 用户名=" + user.getUsername());
        
        // 返回用户信息
            return convertToResponseDTO(user);
        } catch (Exception e) {
            System.err.println("获取当前用户失败: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("=================== 获取当前用户结束 ===================");
        }
    }

    @Override
    public void logout() {
        System.out.println("=================== 用户登出开始 ===================");
        
        try {
            // 打印当前会话信息
            System.out.println("登出前的会话信息:");
            printSessionAttributes();
            
        // 清除session
            Long userId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
            System.out.println("准备登出的用户ID: " + userId);
            
        httpSession.removeAttribute(USER_SESSION_KEY);
        httpSession.invalidate();
            System.out.println("已清除会话信息");
        } catch (Exception e) {
            System.err.println("登出失败: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("=================== 用户登出结束 ===================");
        }
    }
    
    // 打印会话所有属性的辅助方法
    private void printSessionAttributes() {
        System.out.println("会话ID: " + httpSession.getId());
        System.out.println("会话属性列表:");
        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        if (!attributeNames.hasMoreElements()) {
            System.out.println("  (无属性)");
        }
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            System.out.println("  " + name + ": " + httpSession.getAttribute(name));
        }
    }
    
    @Override
    @Transactional
    public UserResponseDTO updateUserInfo(UserUpdateDTO updateDTO) {
        // 获取当前用户
        User user = getCurrentUserEntity();
        
        // 如果要更新邮箱，检查邮箱是否已被其他用户使用
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(updateDTO.getEmail(), user.getId())) {
                throw new RuntimeException("该邮箱已被其他用户注册");
            }
            user.setEmail(updateDTO.getEmail());
        }
        
        // 更新手机号
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        
        // 保存更新后的用户信息
        User updatedUser = userRepository.save(user);
        
        return convertToResponseDTO(updatedUser);
    }
    
    @Override
    @Transactional
    public void updatePassword(PasswordUpdateDTO passwordUpdateDTO) {
        // 获取当前用户
        User user = getCurrentUserEntity();
        
        // 获取用户密码
        UserPassword userPassword = userPasswordRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("用户密码信息不存在"));
        
        // 验证当前密码
        if (!passwordEncoder.matches(passwordUpdateDTO.getCurrentPassword(), userPassword.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        
        // 验证两次新密码是否一致
        if (!passwordUpdateDTO.getNewPassword().equals(passwordUpdateDTO.getConfirmPassword())) {
            throw new RuntimeException("两次新密码输入不一致");
        }
        
        // 验证密码长度
        if (passwordUpdateDTO.getNewPassword().length() < 6) {
            throw new RuntimeException("密码长度至少为6位");
        }
        
        // 更新密码
        userPassword.setPassword(passwordEncoder.encode(passwordUpdateDTO.getNewPassword()));
        userPasswordRepository.save(userPassword);
    }
    
    // 获取当前登录用户实体
    private User getCurrentUserEntity() {
        // 从session获取当前用户ID
        Long userId = (Long) httpSession.getAttribute(USER_SESSION_KEY);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        
        // 查找用户
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    // 将User实体转换为UserResponseDTO
    private UserResponseDTO convertToResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRegisterTime()
        );
    }
} 