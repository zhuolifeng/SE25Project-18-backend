package com.dealwithpapers.dealwithpapers.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/cors")
    public Map<String, Object> testCors() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS配置正常");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "success");
        
        System.out.println("CORS测试端点被调用");
        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "DOI代理服务");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    @GetMapping(value = "/hello", produces = "text/plain;charset=UTF-8")
    public String hello() {
        return "hello!";
    }
} 
} 
