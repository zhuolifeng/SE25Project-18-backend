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
                .allowedOriginPatterns("*")  // 允许所有来源
                .allowedMethods("*")         // 允许所有HTTP方法
                .allowedHeaders("*")         // 允许所有头部
                .exposedHeaders("*")         // 暴露所有头部
                .allowCredentials(true)      // 允许发送凭证
                .maxAge(3600);               // 预检请求缓存时间
                
        System.out.println("CORS配置已加载：允许所有源跨域访问，允许携带凭证");
    }
    
    /**
     * 配置Cookie序列化器，用于会话管理
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");       // 设置Cookie名称
        serializer.setCookiePath("/");               // Cookie路径
        serializer.setCookieMaxAge(3600);            // Cookie最大存活时间（秒）
        serializer.setUseHttpOnlyCookie(false);      // 允许JS访问Cookie
        serializer.setUseSecureCookie(false);        // 开发环境不要求HTTPS
        serializer.setSameSite(null);                // 在局域网环境中使用默认SameSite设置
        
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