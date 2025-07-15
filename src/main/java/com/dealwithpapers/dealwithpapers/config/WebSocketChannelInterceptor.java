package com.dealwithpapers.dealwithpapers.config;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private UserRepository userRepository;

    private static final String USER_SESSION_KEY = "currentUser";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("WebSocket连接请求 - 处理认证");
            
            // 尝试从多个来源获取用户信息
            Long userId = null;
            
            // 1. 尝试从握手拦截器传递的attributes获取userId
            String userIdAttribute = (String) accessor.getSessionAttributes().get("userId");
            if (userIdAttribute != null) {
                try {
                    userId = Long.valueOf(userIdAttribute);
                    System.out.println("从握手属性中获取用户ID: " + userId);
                } catch (NumberFormatException e) {
                    System.out.println("握手属性中的userId格式错误: " + userIdAttribute);
                }
            }
            
            // 2. 尝试从HTTP会话获取
            if (userId == null) {
                HttpSession session = (HttpSession) accessor.getSessionAttributes().get("HTTP_SESSION");
                if (session != null) {
                    userId = (Long) session.getAttribute(USER_SESSION_KEY);
                    System.out.println("从HTTP会话获取用户ID: " + userId);
                }
            }
            
            // 3. 尝试从URL参数获取
            if (userId == null) {
                List<String> userIdParams = accessor.getNativeHeader("userId");
                if (userIdParams != null && !userIdParams.isEmpty()) {
                    try {
                        userId = Long.valueOf(userIdParams.get(0));
                        System.out.println("从Header参数获取用户ID: " + userId);
                    } catch (NumberFormatException e) {
                        System.out.println("Header参数userId格式错误: " + userIdParams.get(0));
                    }
                }
            }
            
            // 4. 尝试从查询参数获取
            if (userId == null) {
                String query = accessor.getFirstNativeHeader("query");
                if (query != null) {
                    System.out.println("查询参数: " + query);
                    // 解析查询参数
                    Map<String, String> queryParams = parseQueryString(query);
                    if (queryParams.containsKey("userId")) {
                        try {
                            userId = Long.valueOf(queryParams.get("userId"));
                            System.out.println("从查询参数获取用户ID: " + userId);
                        } catch (NumberFormatException e) {
                            System.out.println("查询参数userId格式错误: " + queryParams.get("userId"));
                        }
                    }
                }
            }
            
            // 如果获取到了用户ID，验证用户并设置认证信息
            if (userId != null) {
                System.out.println("尝试通过ID查找用户: " + userId);
                // 从数据库查找用户
                Optional<User> userOptional = userRepository.findById(userId);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    System.out.println("找到用户: " + user.getId() + " - " + user.getUsername());
                    
                    // 设置认证信息
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            user, 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    
                    accessor.setUser(authentication);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("已设置用户认证信息");
                } else {
                    System.out.println("未找到用户: " + userId);
                }
            } else {
                System.out.println("无法获取用户ID，WebSocket连接将不会关联用户");
            }
        } else if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            // 记录发送消息的信息
            System.out.println("WebSocket发送消息 - 目标: " + accessor.getDestination());
            System.out.println("消息头: " + accessor.getMessageHeaders());
            
            // 检查是否有用户认证信息
            if (accessor.getUser() != null) {
                System.out.println("发送消息的用户: " + accessor.getUser().getName());
            } else {
                System.out.println("发送消息没有用户认证信息");
                
                // 尝试从消息头获取userId
                List<String> userIdHeaders = accessor.getNativeHeader("userId");
                if (userIdHeaders != null && !userIdHeaders.isEmpty()) {
                    try {
                        Long userId = Long.valueOf(userIdHeaders.get(0));
                        System.out.println("从消息头获取用户ID: " + userId);
                        
                        // 从数据库获取用户并设置认证
                        Optional<User> userOptional = userRepository.findById(userId);
                        if (userOptional.isPresent()) {
                            User user = userOptional.get();
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    user, 
                                    null, 
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                            
                            accessor.setUser(authentication);
                            System.out.println("为发送消息设置用户认证: " + user.getUsername());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("消息头userId格式错误");
                    }
                }
            }
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // 记录订阅信息
            System.out.println("WebSocket订阅 - 目标: " + accessor.getDestination());
            if (accessor.getUser() != null) {
                System.out.println("订阅用户: " + accessor.getUser().getName());
            } else {
                System.out.println("订阅没有用户认证信息");
            }
        }
        
        return message;
    }
    
    /**
     * 解析查询字符串，转换为键值对Map
     */
    private Map<String, String> parseQueryString(String query) {
        if (query == null || query.isEmpty() || !query.contains("=")) {
            return Collections.emptyMap();
        }
        
        Map<String, String> params = new java.util.HashMap<>();
        String[] pairs = query.split("&");
        
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                params.put(key, value);
            }
        }
        
        return params;
    }
} 