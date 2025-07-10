package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PdfExtractResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExtractionService {

    private final RestTemplate restTemplate;
    private final PaperService paperService;
    private final DoiProxyService doiProxyService;
    private final ObjectMapper objectMapper;

    @Value("${python.rag.service.url:http://localhost:8002}")
    private String pythonRagServiceUrl;

    /**
     * 异步提取PDF内容到RAG系统
     * @param paperId 论文ID
     * @return 提取结果的CompletableFuture
     */
    public CompletableFuture<Map<String, Object>> extractPdfContentAsync(Long paperId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始异步提取PDF内容，论文ID: {}", paperId);
                
                // 获取论文信息
                PaperDTO paper = paperService.getPaperById(paperId);
                if (paper == null) {
                    throw new RuntimeException("论文不存在，ID: " + paperId);
                }
                
                // 构建PDF URL
                String pdfUrl = buildPdfUrl(paper);
                if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
                    throw new RuntimeException("无法获取PDF URL");
                }
                
                // 调用Python RAG服务提取内容
                return callPythonRagService(paperId, paper.getTitle(), pdfUrl);
                
            } catch (Exception e) {
                log.error("异步提取PDF内容失败，论文ID: {}", paperId, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "PDF内容提取失败: " + e.getMessage());
                errorResult.put("paperId", paperId);
                errorResult.put("status", "error");
                return errorResult;
            }
        });
    }

    /**
     * 同步提取PDF内容到RAG系统
     * @param paperId 论文ID
     * @return 提取结果
     */
    public Map<String, Object> extractPdfContent(Long paperId) {
        try {
            log.info("开始提取PDF内容，论文ID: {}", paperId);
            
            // 获取论文信息
            PaperDTO paper = paperService.getPaperById(paperId);
            if (paper == null) {
                throw new RuntimeException("论文不存在，ID: " + paperId);
            }
            
            // 构建PDF URL
            String pdfUrl = buildPdfUrl(paper);
            if (pdfUrl == null || pdfUrl.trim().isEmpty()) {
                throw new RuntimeException("无法获取PDF URL");
            }
            
            // 调用Python RAG服务提取内容
            return callPythonRagService(paperId, paper.getTitle(), pdfUrl);
            
        } catch (Exception e) {
            log.error("提取PDF内容失败，论文ID: {}", paperId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "PDF内容提取失败: " + e.getMessage());
            errorResult.put("paperId", paperId);
            errorResult.put("status", "error");
            return errorResult;
        }
    }

    /**
     * 获取PDF提取状态
     * @param paperId 论文ID
     * @return 提取状态
     */
    public Map<String, Object> getPdfExtractionStatus(Long paperId) {
        try {
            log.info("获取PDF提取状态，论文ID: {}", paperId);
            
            String url = pythonRagServiceUrl + "/api/documents/pdf-status/" + paperId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = response.getBody();
                log.info("PDF提取状态获取成功，论文ID: {}, 状态: {}", paperId, result);
                return result;
            } else {
                throw new RuntimeException("获取PDF提取状态失败，HTTP状态: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("获取PDF提取状态失败，HTTP错误，论文ID: {}", paperId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("extracted", false);
            errorResult.put("chunks_count", 0);
            errorResult.put("status", "error");
            errorResult.put("error", "HTTP错误: " + e.getStatusCode());
            return errorResult;
        } catch (ResourceAccessException e) {
            log.error("获取PDF提取状态失败，连接错误，论文ID: {}", paperId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("extracted", false);
            errorResult.put("chunks_count", 0);
            errorResult.put("status", "connection_error");
            errorResult.put("error", "连接Python RAG服务失败");
            return errorResult;
        } catch (Exception e) {
            log.error("获取PDF提取状态失败，论文ID: {}", paperId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("extracted", false);
            errorResult.put("chunks_count", 0);
            errorResult.put("status", "error");
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 构建PDF URL
     * @param paper 论文对象
     * @return PDF URL
     */
    private String buildPdfUrl(PaperDTO paper) {
        try {
            // 优先使用论文对象中的PDF URL
            if (paper.getPdfUrl() != null && !paper.getPdfUrl().trim().isEmpty()) {
                log.info("使用论文对象中的PDF URL: {}", paper.getPdfUrl());
                return paper.getPdfUrl();
            }
            
            // 如果有DOI，尝试提取PDF链接
            if (paper.getDoi() != null && !paper.getDoi().trim().isEmpty()) {
                log.info("尝试从DOI提取PDF链接: {}", paper.getDoi());
                PdfExtractResponseDTO pdfResponse = doiProxyService.extractPdfFromUrl("https://doi.org/" + paper.getDoi());
                if (pdfResponse.isSuccess() && pdfResponse.getPdfUrl() != null) {
                    log.info("从DOI成功提取PDF链接: {}", pdfResponse.getPdfUrl());
                    return pdfResponse.getPdfUrl();
                }
            }
            
            // 如果有URL，尝试提取PDF链接
            if (paper.getUrl() != null && !paper.getUrl().trim().isEmpty()) {
                log.info("尝试从URL提取PDF链接: {}", paper.getUrl());
                PdfExtractResponseDTO pdfResponse = doiProxyService.extractPdfFromUrl(paper.getUrl());
                if (pdfResponse.isSuccess() && pdfResponse.getPdfUrl() != null) {
                    log.info("从URL成功提取PDF链接: {}", pdfResponse.getPdfUrl());
                    return pdfResponse.getPdfUrl();
                }
            }
            
            // 如果是arXiv论文，构建arXiv PDF URL
            if (paper.getUrl() != null && paper.getUrl().contains("arxiv.org")) {
                String arxivUrl = convertToArxivPdfUrl(paper.getUrl());
                if (arxivUrl != null) {
                    log.info("构建arXiv PDF URL: {}", arxivUrl);
                    return arxivUrl;
                }
            }
            
            log.warn("无法构建PDF URL，论文ID: {}, 标题: {}", paper.getId(), paper.getTitle());
            return null;
            
        } catch (Exception e) {
            log.error("构建PDF URL失败，论文ID: {}", paper.getId(), e);
            return null;
        }
    }

    /**
     * 将arXiv URL转换为PDF URL
     * @param url arXiv URL
     * @return PDF URL
     */
    private String convertToArxivPdfUrl(String url) {
        try {
            // 提取arXiv ID
            String arxivId = null;
            if (url.contains("/abs/")) {
                arxivId = url.substring(url.lastIndexOf("/abs/") + 5);
            } else if (url.contains("/pdf/")) {
                arxivId = url.substring(url.lastIndexOf("/pdf/") + 5);
                if (arxivId.endsWith(".pdf")) {
                    arxivId = arxivId.substring(0, arxivId.length() - 4);
                }
            }
            
            if (arxivId != null && !arxivId.trim().isEmpty()) {
                return "https://arxiv.org/pdf/" + arxivId + ".pdf";
            }
            
            return null;
        } catch (Exception e) {
            log.error("转换arXiv PDF URL失败: {}", url, e);
            return null;
        }
    }

    /**
     * 调用Python RAG服务
     * @param paperId 论文ID
     * @param paperTitle 论文标题
     * @param pdfUrl PDF URL
     * @return 调用结果
     */
    private Map<String, Object> callPythonRagService(Long paperId, String paperTitle, String pdfUrl) {
        try {
            String url = pythonRagServiceUrl + "/api/documents/extract-pdf";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("pdf_url", pdfUrl);
            requestBody.put("paper_id", paperId);
            requestBody.put("paper_title", paperTitle);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用Python RAG服务，URL: {}, 请求体: {}", url, requestBody);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = response.getBody();
                log.info("Python RAG服务调用成功，结果: {}", result);
                return result;
            } else {
                throw new RuntimeException("Python RAG服务调用失败，HTTP状态: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Python RAG服务调用失败，HTTP错误", e);
            throw new RuntimeException("Python RAG服务调用失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("Python RAG服务调用失败，连接错误", e);
            throw new RuntimeException("连接Python RAG服务失败，请检查服务是否运行");
        } catch (Exception e) {
            log.error("Python RAG服务调用失败", e);
            throw new RuntimeException("Python RAG服务调用失败: " + e.getMessage());
        }
    }
}