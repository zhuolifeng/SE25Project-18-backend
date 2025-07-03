package com.dealwithpapers.dealwithpapers.config;

import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.util.AuthUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;
    
    private static final String USER_SESSION_KEY = "currentUser";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(sessionAuthenticationFilter(), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register", "/api/users/login", "/api/users/current", "/api/users/session-test").permitAll()
                .requestMatchers("/api/papers/search", "/api/papers/search/**").permitAll()
                .requestMatchers("/api/papers", "/api/papers/**").permitAll()
                .requestMatchers("/api/favorites/**").permitAll()
                .requestMatchers("/api/posts/search", "/api/posts/search/**").permitAll()
                .requestMatchers("/api/posts/**").permitAll()
                .requestMatchers("/api/history/**").permitAll()
                .requestMatchers("/api/tags/**").permitAll()
                .requestMatchers("/api/comments/**").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers(request -> "OPTIONS".equals(request.getMethod())).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        
        return http.build();
    }

    /**
     * 会话认证过滤器，从会话中获取用户信息并设置到SecurityContext
     */
    @Bean
    public OncePerRequestFilter sessionAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                // 获取当前会话
                HttpSession session = request.getSession(false);
                
                // 调试信息
                System.out.println("=== 会话认证过滤器 ===");
                System.out.println("请求路径: " + request.getRequestURI());
                System.out.println("会话存在: " + (session != null));
                
                // 如果会话存在且包含用户ID
                if (session != null) {
                    Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
                    System.out.println("会话用户ID: " + userId);
                    
                    if (userId != null) {
                        // 从数据库获取用户信息
                        Optional<User> userOptional = userRepository.findById(userId);
                        
                        if (userOptional.isPresent()) {
                            User user = userOptional.get();
                            System.out.println("从数据库获取到用户: " + user.getUsername());
                            
                            // 创建认证对象并设置到SecurityContext
                            Authentication authentication = new UserAuthentication(user);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("已设置用户认证信息到SecurityContext");
                        } else {
                            System.out.println("数据库中找不到用户: " + userId);
                        }
                    }
                }
                
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", 
                                                    "Accept", "Origin", "Access-Control-Request-Method",
                                                    "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Content-Length", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 自定义用户认证对象
     */
    public static class UserAuthentication implements Authentication {
        private final User user;
        private boolean authenticated = true;
        
        public UserAuthentication(User user) {
            this.user = user;
        }
        
        @Override
        public String getName() {
            return user.getUsername();
        }
        
        @Override
        public Object getCredentials() {
            return null; // 不需要凭证
        }
        
        @Override
        public Object getDetails() {
            return user;
        }
        
        @Override
        public Object getPrincipal() {
            return user; // 用户实体作为主体
        }
        
        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }
        
        @Override
        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }
        
        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return AuthorityUtils.createAuthorityList("ROLE_USER");
        }
    }
} 