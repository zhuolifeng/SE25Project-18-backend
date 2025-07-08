package com.dealwithpapers.dealwithpapers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type", "Content-Length", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
                .allowCredentials(true)
                .maxAge(3600);
                
        System.out.println("CORS配置已加载：允许http://localhost:3000跨域访问，允许携带凭证");
    }
    
    /**
     * 配置Cookie序列化器，用于会话管理
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");           // 设置Cookie名称
        serializer.setCookiePath("/");                   // Cookie路径
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$"); // 允许子域名共享Cookie
        serializer.setCookieMaxAge(3600);                // Cookie最大存活时间（秒）
        serializer.setUseHttpOnlyCookie(false);          // 允许JS访问Cookie
        serializer.setUseSecureCookie(false);            // 开发环境不要求HTTPS
        serializer.setSameSite(null);                    // 允许跨站请求
        
        System.out.println("Cookie序列化器配置完成: cookieName=SESSIONID, maxAge=3600秒");
        return serializer;
    }
    
    /**
     * 配置消息转换器，确保UTF-8编码
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, stringHttpMessageConverter());
    }
    
    /**
     * 字符串消息转换器
     */
    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }
    
    /**
     * 配置Jackson JSON转换器
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter = 
                    (MappingJackson2HttpMessageConverter) converter;
                jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * 配置静态资源映射，支持头像等图片访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
} 