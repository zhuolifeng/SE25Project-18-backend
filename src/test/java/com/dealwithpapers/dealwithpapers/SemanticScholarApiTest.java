package com.dealwithpapers.dealwithpapers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;

/**
 * 测试 Semantic Scholar API 返回格式
 */
@SpringBootTest
public class SemanticScholarApiTest {
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testSemanticScholarApiResponse() throws IOException, InterruptedException {
        // 测试搜索论文
        testSearchPaper();
        
        // 测试获取论文详情（包含引用和被引用）
        testGetPaperDetails();
        
        // 测试 ArXiv 论文
        testArxivPaper();
    }
    
    /**
     * 测试搜索论文
     */
    private void testSearchPaper() throws IOException, InterruptedException {
        String query = URLEncoder.encode("machine learning", StandardCharsets.UTF_8);
        String searchUrl = "https://api.semanticscholar.org/graph/v1/paper/search/bulk?" +
                "query=" + query + "&" +
                "fields=paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract&" +
                "limit=2";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("=== 搜索论文结果 ===");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
        System.out.println();
        
        // 解析JSON以验证结构
        if (response.statusCode() == 200) {
            try {
                Object jsonObject = objectMapper.readValue(response.body(), Object.class);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                System.out.println("格式化JSON: " + prettyJson);
            } catch (Exception e) {
                System.err.println("JSON解析错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试获取论文详情（包含引用和被引用）
     */
    private void testGetPaperDetails() throws IOException, InterruptedException {
        // 使用一个已知的论文ID
        String paperId = "649def34f8be52c8b66281af98ae884c09aef38b";
        String detailUrl = "https://api.semanticscholar.org/graph/v1/paper/" + paperId + "?" +
                "fields=paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract,references,citations";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(detailUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("=== 论文详情结果 ===");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
        System.out.println();
        
        // 解析JSON以验证结构
        if (response.statusCode() == 200) {
            try {
                Object jsonObject = objectMapper.readValue(response.body(), Object.class);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                System.out.println("格式化JSON: " + prettyJson);
            } catch (Exception e) {
                System.err.println("JSON解析错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试 ArXiv 论文
     */
    private void testArxivPaper() throws IOException, InterruptedException {
        // 测试 ArXiv 论文
        String arxivUrl = "https://api.semanticscholar.org/graph/v1/paper/ARXIV:2301.00001?" +
                "fields=paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract,references,citations,openAccessPdf";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(arxivUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("=== ArXiv 论文结果 ===");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
        System.out.println();
        
        // 解析JSON以验证结构
        if (response.statusCode() == 200) {
            try {
                Object jsonObject = objectMapper.readValue(response.body(), Object.class);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                System.out.println("格式化JSON: " + prettyJson);
            } catch (Exception e) {
                System.err.println("JSON解析错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试通过DOI查找论文
     */
    @Test
    public void testSearchByDoi() throws IOException, InterruptedException {
        String doi = "10.1038/nature14539";
        String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        String searchUrl = "https://api.semanticscholar.org/graph/v1/paper/search/bulk?" +
                "query=" + encodedDoi + "&" +
                "fields=paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract&" +
                "limit=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("=== 通过DOI搜索结果 ===");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
        System.out.println();
    }
    
    /**
     * 测试通过论文标题搜索
     */
    @Test
    public void testSearchByTitle() throws IOException, InterruptedException {
        String title = "Attention Is All You Need";
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String searchUrl = "https://api.semanticscholar.org/graph/v1/paper/search/bulk?" +
                "query=" + encodedTitle + "&" +
                "fields=paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract&" +
                "limit=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("=== 通过标题搜索结果 ===");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
        System.out.println();
    }
}