package com.dealwithpapers.dealwithpapers.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.context.annotation.Bean;

/**
 * 聊天代理控制器
 * 将聊天请求转发到Python RAG服务
 */
@RestController
@RequestMapping("/api/chat")
public class ChatProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatProxyController.class);

    @Value("${python.rag.service.url:http://localhost:8002}")
    private String pythonServiceUrl;
    
    // 使用默认配置的RestTemplate，避免依赖问题
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * 代理聊天查询到Python服务
     */
    @PostMapping("/query")
    public ResponseEntity<?> chatQuery(@RequestBody Object request) {
        try {
            // 记录请求
            logger.info("转发聊天查询请求到Python服务");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 创建请求实体
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            // 使用exchange方法
            ResponseEntity<?> response = restTemplate().exchange(
                pythonServiceUrl + "/api/chat/query",
                HttpMethod.POST,
                entity,
                Object.class
            );
            return response;
        } catch (Exception e) {
            logger.error("聊天查询请求失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "服务器内部错误: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 代理清除对话历史到Python服务
     */
    @PostMapping("/clear")
    public ResponseEntity<?> clearConversation(@RequestBody Object request) {
        try {
            String url = pythonServiceUrl + "/api/chat/clear";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate().exchange(
                url, HttpMethod.POST, entity, Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("清除对话服务暂时不可用: " + e.getMessage());
        }
    }
    
    /**
     * 代理文档处理到Python服务
     */
    @PostMapping("/documents/process")
    public ResponseEntity<?> processDocuments(@RequestBody Object request) {
        try {
            // 修改URL路径，应该是 /api/documents/process 而不是 /api/chat/documents/process
            String url = pythonServiceUrl + "/api/documents/process";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate().exchange(
                url, HttpMethod.POST, entity, Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("文档处理服务暂时不可用: " + e.getMessage());
        }
    }
    
    /**
     * 代理PDF提取请求到Python服务
     */
    @PostMapping("/documents/extract-pdf")
    public ResponseEntity<?> extractPdf(@RequestBody Object request) {
        try {
            logger.info("转发PDF提取请求到Python服务");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<?> response = restTemplate().exchange(
                pythonServiceUrl + "/api/documents/extract-pdf", 
                HttpMethod.POST, 
                entity,
                Object.class
            );
            
            logger.info("PDF提取请求转发成功，状态码: " + response.getStatusCode());
            return response;
            
        } catch (Exception e) {
            logger.error("PDF提取请求转发失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "PDF处理服务暂时不可用: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 代理向量数据库调试请求到Python服务
     */
    @GetMapping("/documents/debug-vector/{paper_id}")
    public ResponseEntity<?> debugVector(@PathVariable String paper_id) {
        try {
            logger.info("转发向量库调试请求到Python服务，论文ID: " + paper_id);
            
            ResponseEntity<?> response = restTemplate().getForEntity(
                pythonServiceUrl + "/api/documents/debug-vector/" + paper_id,
                Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            logger.error("向量库调试请求转发失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "向量库调试服务暂时不可用: " + e.getMessage()
            ));
        }
    }
} 