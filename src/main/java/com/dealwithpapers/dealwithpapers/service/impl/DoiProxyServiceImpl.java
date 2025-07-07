package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.DoiProxyResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.PdfExtractResponseDTO;
import com.dealwithpapers.dealwithpapers.service.DoiProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoiProxyServiceImpl implements DoiProxyService {

    private final WebClient webClient;

    @Override
    public DoiProxyResponseDTO proxyDoiRequest(String doi) {
        log.info("开始代理DOI请求: {}", doi);
        
        try {
            // 清理DOI格式
            String cleanDoi = cleanDoi(doi);
            String doiUrl = "https://doi.org/" + cleanDoi;
            
            log.info("向DOI服务器发送HEAD请求: {}", doiUrl);
            
            // 发送HEAD请求获取重定向URL
            String redirectUrl = webClient.method(HttpMethod.HEAD)
                    .uri(doiUrl)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(30))
                    .map(responseEntity -> {
                        // 从响应头中获取Location或者从URI中获取最终URL
                        String location = responseEntity.getHeaders().getFirst("Location");
                        if (location != null && !location.isEmpty()) {
                            return location;
                        }
                        // 如果没有Location头，返回原始URL
                        return doiUrl;
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("DOI请求失败，状态码: {}, 错误: {}", ex.getStatusCode(), ex.getMessage());
                        if (ex.getStatusCode() == HttpStatus.MOVED_PERMANENTLY || 
                            ex.getStatusCode() == HttpStatus.FOUND ||
                            ex.getStatusCode() == HttpStatus.SEE_OTHER ||
                            ex.getStatusCode() == HttpStatus.TEMPORARY_REDIRECT) {
                            // 从错误响应中获取Location头
                            String location = ex.getHeaders().getFirst("Location");
                            if (location != null && !location.isEmpty()) {
                                return Mono.just(location);
                            }
                        }
                        return Mono.just(doiUrl);
                    })
                    .onErrorReturn(doiUrl)
                    .block();
            
            log.info("DOI代理请求成功，重定向URL: {}", redirectUrl);
            
            return new DoiProxyResponseDTO(true, cleanDoi, redirectUrl, null, "DOI代理请求成功");
            
        } catch (Exception e) {
            log.error("DOI代理请求失败: {}", e.getMessage(), e);
            return new DoiProxyResponseDTO(false, doi, null, e.getMessage(), "DOI代理请求失败");
        }
    }

    @Override
    public PdfExtractResponseDTO extractPdfFromUrl(String url) {
        log.info("开始从URL提取PDF: {}", url);
        
        try {
            String pdfUrl = null;
            Map<String, Object> metadata = new HashMap<>();
            
            // 检查URL类型并提取PDF链接
            if (url.contains("doi.org") || isDoi(url)) {
                // 处理DOI URL
                pdfUrl = extractPdfFromDoi(url, metadata);
            } else if (url.contains("arxiv.org")) {
                // 处理ArXiv URL
                pdfUrl = extractPdfFromArxiv(url, metadata);
            } else if (url.contains("ieee.org")) {
                // 处理IEEE URL
                pdfUrl = extractPdfFromIeee(url, metadata);
            } else if (url.contains("acm.org")) {
                // 处理ACM URL
                pdfUrl = extractPdfFromAcm(url, metadata);
            } else if (url.contains("springer.com")) {
                // 处理Springer URL
                pdfUrl = extractPdfFromSpringer(url, metadata);
            } else {
                // 通用处理
                pdfUrl = extractPdfGeneric(url, metadata);
            }
            
            if (pdfUrl != null) {
                log.info("PDF提取成功: {}", pdfUrl);
                return new PdfExtractResponseDTO(true, pdfUrl, metadata, url, null, "PDF提取成功");
            } else {
                log.warn("未能从URL提取PDF: {}", url);
                return new PdfExtractResponseDTO(false, null, metadata, url, "未找到PDF链接", "未能从URL提取PDF");
            }
            
        } catch (Exception e) {
            log.error("PDF提取失败: {}", e.getMessage(), e);
            return new PdfExtractResponseDTO(false, null, null, url, e.getMessage(), "PDF提取失败");
        }
    }
    
    /**
     * 清理DOI格式
     */
    private String cleanDoi(String doi) {
        if (doi == null || doi.isEmpty()) {
            return doi;
        }
        
        // 移除前缀
        doi = doi.replaceAll("^(https?://)?(?:dx\\.)?doi\\.org/", "");
        doi = doi.replaceAll("^doi:", "");
        
        return doi.trim();
    }
    
    /**
     * 检查是否为DOI格式
     */
    private boolean isDoi(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        Pattern doiPattern = Pattern.compile("10\\.\\d{4,}/[^\\s]+");
        return doiPattern.matcher(text).find();
    }
    
    /**
     * 从DOI提取PDF
     */
    private String extractPdfFromDoi(String url, Map<String, Object> metadata) {
        try {
            // 提取DOI
            String doi = extractDoiFromUrl(url);
            if (doi == null) {
                return null;
            }
            
            metadata.put("doi", doi);
            metadata.put("source", "doi");
            
            // 尝试Unpaywall API
            String unpaywallUrl = tryUnpaywallApi(doi, metadata);
            if (unpaywallUrl != null) {
                return unpaywallUrl;
            }
            
            // 备选方案：Sci-Hub
            metadata.put("source", "sci-hub");
            metadata.put("needsProxy", true);
            return "https://sci-hub.se/" + doi;
            
        } catch (Exception e) {
            log.error("从DOI提取PDF失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从ArXiv提取PDF
     */
    private String extractPdfFromArxiv(String url, Map<String, Object> metadata) {
        try {
            Pattern arxivPattern = Pattern.compile("(\\d{4}\\.\\d{4,5})(v\\d+)?");
            Matcher matcher = arxivPattern.matcher(url);
            
            if (matcher.find()) {
                String arxivId = matcher.group(1);
                metadata.put("arxivId", arxivId);
                metadata.put("source", "arxiv");
                
                return "https://arxiv.org/pdf/" + arxivId + ".pdf";
            }
            
            return null;
        } catch (Exception e) {
            log.error("从ArXiv提取PDF失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从IEEE提取PDF
     */
    private String extractPdfFromIeee(String url, Map<String, Object> metadata) {
        try {
            Pattern ieeePattern = Pattern.compile("document/(\\d+)");
            Matcher matcher = ieeePattern.matcher(url);
            
            if (matcher.find()) {
                String documentId = matcher.group(1);
                metadata.put("ieeeDocumentId", documentId);
                metadata.put("source", "ieee");
                metadata.put("needsProxy", true);
                
                return "https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=" + documentId;
            }
            
            return null;
        } catch (Exception e) {
            log.error("从IEEE提取PDF失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从ACM提取PDF
     */
    private String extractPdfFromAcm(String url, Map<String, Object> metadata) {
        try {
            Pattern acmPattern = Pattern.compile("doi/(10\\.1145/[^/]+)");
            Matcher matcher = acmPattern.matcher(url);
            
            if (matcher.find()) {
                String doi = matcher.group(1);
                metadata.put("doi", doi);
                metadata.put("source", "acm");
                metadata.put("needsProxy", true);
                
                return "https://dl.acm.org/doi/pdf/" + doi;
            }
            
            return null;
        } catch (Exception e) {
            log.error("从ACM提取PDF失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从Springer提取PDF
     */
    private String extractPdfFromSpringer(String url, Map<String, Object> metadata) {
        try {
            Pattern springerPattern = Pattern.compile("article/(10\\.1007/[^/]+)");
            Matcher matcher = springerPattern.matcher(url);
            
            if (matcher.find()) {
                String doi = matcher.group(1);
                metadata.put("doi", doi);
                metadata.put("source", "springer");
                metadata.put("needsProxy", true);
                
                return "https://link.springer.com/content/pdf/" + doi + ".pdf";
            }
            
            return null;
        } catch (Exception e) {
            log.error("从Springer提取PDF失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 通用PDF提取
     */
    private String extractPdfGeneric(String url, Map<String, Object> metadata) {
        try {
            metadata.put("source", "generic");
            
            // 如果URL本身就是PDF
            if (url.toLowerCase().endsWith(".pdf")) {
                return url;
            }
            
            // 尝试添加PDF扩展名
            if (!url.contains("?")) {
                return url + ".pdf";
            }
            
            return null;
        } catch (Exception e) {
            log.error("通用PDF提取失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从URL提取DOI
     */
    private String extractDoiFromUrl(String url) {
        Pattern doiPattern = Pattern.compile("(10\\.\\d{4,}/[^\\s]+)");
        Matcher matcher = doiPattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 尝试Unpaywall API
     */
    private String tryUnpaywallApi(String doi, Map<String, Object> metadata) {
        try {
            String unpaywallUrl = "https://api.unpaywall.org/v2/" + doi + "?email=paperviewer@academicresearch.org";
            
            log.info("尝试Unpaywall API: {}", unpaywallUrl);
            
            // 这里可以实现Unpaywall API调用
            // 由于API限制，这里暂时返回null
            // 实际实现中可以调用Unpaywall API获取开放获取的PDF链接
            
            return null;
        } catch (Exception e) {
            log.error("Unpaywall API调用失败: {}", e.getMessage(), e);
            return null;
        }
    }
} 