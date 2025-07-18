package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserFollow;
import com.dealwithpapers.dealwithpapers.entity.UserPassword;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserFollowRepository;
import com.dealwithpapers.dealwithpapers.repository.UserPasswordRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPasswordRepository userPasswordRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<UserPassword> userPasswordCaptor;

    private UserRegisterDTO validRegisterDTO;
    private UserLoginDTO validLoginDTO;
    private User mockUser;
    private UserPassword mockUserPassword;

    @BeforeEach
    void setUp() {
        // 设置有效的注册DTO
        validRegisterDTO = new UserRegisterDTO();
        validRegisterDTO.setUsername("testuser");
        validRegisterDTO.setEmail("test@example.com");
        validRegisterDTO.setPhone("12345678901");
        validRegisterDTO.setPassword("password123");
        validRegisterDTO.setConfirmPassword("password123");

        // 设置有效的登录DTO
        validLoginDTO = new UserLoginDTO();
        validLoginDTO.setUsername("testuser");
        validLoginDTO.setPassword("password123");

        // 设置模拟用户
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setPhone("12345678901");
        mockUser.setRegisterTime(LocalDateTime.now());

        // 设置模拟用户密码
        mockUserPassword = new UserPassword();
        mockUserPassword.setUser(mockUser);
        mockUserPassword.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("注册成功 - 所有字段有效")
    void register_WithValidData_ShouldRegisterUser() {
        // 设置模拟行为
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // 执行测试
        UserResponseDTO result = userService.register(validRegisterDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        // 验证交互
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(userCaptor.capture());
        verify(userPasswordRepository).save(userPasswordCaptor.capture());
        verify(passwordEncoder).encode("password123");

        // 验证捕获的参数
        User capturedUser = userCaptor.getValue();
        assertEquals("testuser", capturedUser.getUsername());
        assertEquals("test@example.com", capturedUser.getEmail());
        assertEquals("12345678901", capturedUser.getPhone());
        assertNotNull(capturedUser.getRegisterTime());

        UserPassword capturedPassword = userPasswordCaptor.getValue();
        assertEquals(mockUser, capturedPassword.getUser());
        assertEquals("encodedPassword", capturedPassword.getPassword());
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_WithExistingUsername_ShouldThrowException() {
        // 设置模拟行为
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.register(validRegisterDTO);
        });

        // 验证异常消息
        assertEquals("用户名已存在", exception.getMessage());

        // 验证交互
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
        verify(userPasswordRepository, never()).save(any(UserPassword.class));
    }

    @Test
    @DisplayName("注册失败 - 邮箱已存在")
    void register_WithExistingEmail_ShouldThrowException() {
        // 设置模拟行为
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.register(validRegisterDTO);
        });

        // 验证异常消息
        assertEquals("邮箱已被注册", exception.getMessage());

        // 验证交互
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(userPasswordRepository, never()).save(any(UserPassword.class));
    }

    @Test
    @DisplayName("注册失败 - 密码不匹配")
    void register_WithPasswordMismatch_ShouldThrowException() {
        // 修改确认密码使其不匹配
        validRegisterDTO.setConfirmPassword("differentPassword");

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.register(validRegisterDTO);
        });

        // 验证异常消息
        assertEquals("两次密码不一致", exception.getMessage());

        // 验证交互
        verify(userRepository, never()).save(any(User.class));
        verify(userPasswordRepository, never()).save(any(UserPassword.class));
    }

    @Test
    @DisplayName("注册失败 - 密码太短")
    void register_WithShortPassword_ShouldThrowException() {
        // 设置短密码
        validRegisterDTO.setPassword("short");
        validRegisterDTO.setConfirmPassword("short");

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.register(validRegisterDTO);
        });

        // 验证异常消息
        assertEquals("密码长度至少为6位", exception.getMessage());

        // 验证交互
        verify(userRepository, never()).save(any(User.class));
        verify(userPasswordRepository, never()).save(any(UserPassword.class));
    }

    @Test
    @DisplayName("登录成功 - 有效凭据")
    void login_WithValidCredentials_ShouldLoginUser() {
        // 设置模拟行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(userPasswordRepository.findById(1L)).thenReturn(Optional.of(mockUserPassword));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // 执行测试
        UserResponseDTO result = userService.login(validLoginDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        // 验证交互
        verify(userRepository).findByUsername("testuser");
        verify(userPasswordRepository).findById(1L);
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(httpSession).setAttribute(eq("currentUser"), eq(1L));
    }

    @Test
    @DisplayName("登录失败 - 用户名不存在")
    void login_WithNonExistentUsername_ShouldThrowException() {
        // 设置模拟行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.login(validLoginDTO);
        });

        // 验证异常消息
        assertEquals("用户名或密码错误", exception.getMessage());

        // 验证交互
        verify(userRepository).findByUsername("testuser");
        verify(userPasswordRepository, never()).findById(anyLong());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(httpSession, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_WithIncorrectPassword_ShouldThrowException() {
        // 设置错误的密码
        validLoginDTO.setPassword("wrongpassword");

        // 设置模拟行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(userPasswordRepository.findById(1L)).thenReturn(Optional.of(mockUserPassword));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.login(validLoginDTO);
        });

        // 验证异常消息
        assertEquals("用户名或密码错误", exception.getMessage());

        // 验证交互
        verify(userRepository).findByUsername("testuser");
        verify(userPasswordRepository).findById(1L);
        verify(passwordEncoder).matches("wrongpassword", "encodedPassword");
        verify(httpSession, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("获取当前用户 - 已登录")
    void getCurrentUser_WhenLoggedIn_ShouldReturnUser() {
        // 设置模拟行为
        when(httpSession.getAttribute("currentUser")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // 执行测试
        UserResponseDTO result = userService.getCurrentUser();

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        // 验证交互
        verify(httpSession).getAttribute("currentUser");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("获取当前用户 - 未登录")
    void getCurrentUser_WhenNotLoggedIn_ShouldThrowException() {
        // 设置模拟行为
        when(httpSession.getAttribute("currentUser")).thenReturn(null);

        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUser();
        });

        // 验证异常消息
        assertEquals("用户未登录", exception.getMessage());

        // 验证交互
        verify(httpSession).getAttribute("currentUser");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("登出 - 应该清除会话")
    void logout_ShouldClearSession() {
        // 执行测试
        userService.logout();

        // 验证交互
        verify(httpSession).removeAttribute("currentUser");
        verify(httpSession).invalidate();
    }
} 