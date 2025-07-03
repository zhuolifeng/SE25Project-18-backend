package com.dealwithpapers.dealwithpapers.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 聊天代理控制器
 * 将聊天请求转发到Python RAG服务
 */
@RestController
@RequestMapping("/api/chat")
public class ChatProxyController {
    
    @Value("${python.rag.service.url:http://localhost:8002}")
    private String pythonServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 代理聊天查询到Python服务
     */
    @PostMapping("/query")
    public ResponseEntity<?> chatQuery(@RequestBody Object request) {
        try {
            String url = pythonServiceUrl + "/api/chat/query";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("聊天服务暂时不可用: " + e.getMessage());
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
            
            ResponseEntity<Object> response = restTemplate.exchange(
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
            String url = pythonServiceUrl + "/api/chat/documents/process";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Object.class
            );
            
            return response;
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("文档处理服务暂时不可用: " + e.getMessage());
        }
    }
} 