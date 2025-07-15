package com.dealwithpapers.dealwithpapers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketChannelInterceptor webSocketChannelInterceptor;
    
    @Bean
    public SimpMessagingTemplate messagingTemplate(SimpMessagingTemplate simpMessagingTemplate) {
        simpMessagingTemplate.setDefaultDestination("/topic/global");
        return simpMessagingTemplate;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于将消息从服务端发送到客户端
        // 客户端订阅 /topic 和 /queue 前缀的目的地时会路由到消息代理
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[] {5000, 5000}) // 设置心跳间隔为5秒，提高实时性
              .setTaskScheduler(heartbeatScheduler()); // 设置心跳任务调度器
        
        // 客户端向服务器发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置点对点消息前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 添加一个端点，客户端通过这个端点连接到 WebSocket 服务器
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许任何源
                .addInterceptors(sessionHandshakeInterceptor()) // 添加自定义握手拦截器
                .withSockJS()  // 启用 SockJS 作为备用方案
                .setDisconnectDelay(15 * 1000) // 设置断开连接延迟为15秒，更快检测断开连接
                .setHeartbeatTime(10 * 1000)   // 设置心跳间隔为10秒，提高连接保持的实时性
                .setStreamBytesLimit(512 * 1024) // 设置流字节限制
                .setHttpMessageCacheSize(1000)   // 设置HTTP消息缓存大小
                .setWebSocketEnabled(true);      // 启用WebSocket
    }

    @Bean
    public HandshakeInterceptor sessionHandshakeInterceptor() {
        HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, 
                                           org.springframework.http.server.ServerHttpResponse response, 
                                           org.springframework.web.socket.WebSocketHandler wsHandler, 
                                           Map<String, Object> attributes) throws Exception {
                System.out.println("=== WebSocket握手拦截器 ===");
                System.out.println("握手请求URL: " + request.getURI());
                System.out.println("请求头: " + request.getHeaders());
                
                // 确保响应头不缓存
                response.getHeaders().setCacheControl("no-cache, no-store, must-revalidate");
                response.getHeaders().setPragma("no-cache");
                response.getHeaders().setExpires(0);
                
                // 获取查询参数
                String query = request.getURI().getQuery();
                if (query != null) {
                    System.out.println("查询参数: " + query);
                    
                    // 如果URL包含userId参数，添加到会话属性中
                    if (query.contains("userId=")) {
                        String userId = query.substring(query.indexOf("userId=") + 7);
                        if (userId.contains("&")) {
                            userId = userId.substring(0, userId.indexOf("&"));
                        }
                        System.out.println("从URL获取到userId: " + userId);
                        attributes.put("userId", userId);
                    }
                }
                
                return super.beforeHandshake(request, response, wsHandler, attributes);
            }
            
            @Override
            public void afterHandshake(org.springframework.http.server.ServerHttpRequest request, 
                                       org.springframework.http.server.ServerHttpResponse response, 
                                       org.springframework.web.socket.WebSocketHandler wsHandler, 
                                       Exception ex) {
                System.out.println("WebSocket握手完成");
                super.afterHandshake(request, response, wsHandler, ex);
            }
        };
        
        // 配置拦截器复制的属性
        interceptor.setCopyAllAttributes(true);
        return interceptor;
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 增加消息大小限制，提高传输效率
        registration.setMessageSizeLimit(128 * 1024); // 128KB的消息大小上限
        registration.setSendBufferSizeLimit(1024 * 1024); // 1MB的发送缓冲区大小
        registration.setSendTimeLimit(15 * 1000); // 15秒的发送超时
        
        // 增加消息发送限流阈值
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB发送缓冲区
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加通道拦截器以处理入站消息
        registration.interceptors(webSocketChannelInterceptor);
        // 增加并发处理能力
        registration.taskExecutor()
            .corePoolSize(4)   // 核心线程数
            .maxPoolSize(10)   // 最大线程数
            .queueCapacity(50) // 队列容量
            .keepAliveSeconds(60); // 线程空闲时间
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 增加并发处理能力
        registration.taskExecutor()
            .corePoolSize(4)   // 核心线程数
            .maxPoolSize(10)   // 最大线程数
            .queueCapacity(50) // 队列容量
            .keepAliveSeconds(60); // 线程空闲时间
    }
    
    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        scheduler.setDaemon(true);
        return scheduler;
    }
} 