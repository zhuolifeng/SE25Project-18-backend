package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperRelationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 引用数据获取服务
 * 封装Semantic Scholar API调用逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitationDataService {
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String SEMANTIC_SCHOLAR_BASE_URL = "https://api.semanticscholar.org/graph/v1";
    private static final String PAPER_FIELDS = "paperId,title,authors,year,doi,citationCount,influentialCitationCount,venue,abstract,references,citations,openAccessPdf";
    
    /**
     * 通过论文标题获取引用数据
     * @param title 论文标题
     * @return 引用关系数据
     */
    public PaperRelationDto getCitationDataByTitle(String title) {
        try {
            log.info("开始获取论文引用数据，标题: {}", title);
            
            // 首先搜索论文获取paperId
            String paperId = searchPaperByTitle(title);
            if (paperId == null) {
                log.warn("未找到论文: {}", title);
                return null;
            }
            
            // 获取详细的引用数据
            return getCitationDataByPaperId(paperId);
            
        } catch (Exception e) {
            log.error("获取引用数据失败，标题: {}", title, e);
            return null;
        }
    }
    
    /**
     * 通过DOI获取引用数据
     * @param doi 论文DOI
     * @return 引用关系数据
     */
    public PaperRelationDto getCitationDataByDoi(String doi) {
        try {
            log.info("开始获取论文引用数据，DOI: {}", doi);
            
            // 首先通过DOI搜索论文获取paperId
            String paperId = searchPaperByDoi(doi);
            if (paperId == null) {
                log.warn("未找到论文: {}", doi);
                return null;
            }
            
            // 获取详细的引用数据
            return getCitationDataByPaperId(paperId);
            
        } catch (Exception e) {
            log.error("获取引用数据失败，DOI: {}", doi, e);
            return null;
        }
    }
    
    /**
     * 通过Semantic Scholar paperId获取引用数据
     * @param paperId Semantic Scholar论文ID
     * @return 引用关系数据
     */
    public PaperRelationDto getCitationDataByPaperId(String paperId) {
        try {
            log.info("开始获取论文引用数据，paperId: {}", paperId);
            
            String detailUrl = SEMANTIC_SCHOLAR_BASE_URL + "/paper/" + paperId + "?" +
                    "fields=" + PAPER_FIELDS;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(detailUrl))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                return parseCitationData(rootNode);
            } else {
                log.warn("API调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取引用数据失败，paperId: {}", paperId, e);
            return null;
        }
    }
    
    /**
     * 通过标题搜索论文获取paperId
     */
    private String searchPaperByTitle(String title) throws IOException, InterruptedException {
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String searchUrl = SEMANTIC_SCHOLAR_BASE_URL + "/paper/search/bulk?" +
                "query=" + encodedTitle + "&" +
                "fields=paperId,title&" +
                "limit=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode dataNode = rootNode.get("data");
            if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                JsonNode firstResult = dataNode.get(0);
                JsonNode paperIdNode = firstResult.get("paperId");
                if (paperIdNode != null) {
                    return paperIdNode.asText();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 通过DOI搜索论文获取paperId
     */
    private String searchPaperByDoi(String doi) throws IOException, InterruptedException {
        String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8);
        String searchUrl = SEMANTIC_SCHOLAR_BASE_URL + "/paper/search/bulk?" +
                "query=" + encodedDoi + "&" +
                "fields=paperId,doi&" +
                "limit=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode dataNode = rootNode.get("data");
            if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                JsonNode firstResult = dataNode.get(0);
                JsonNode paperIdNode = firstResult.get("paperId");
                if (paperIdNode != null) {
                    return paperIdNode.asText();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 解析Semantic Scholar API返回的引用数据
     */
    private PaperRelationDto parseCitationData(JsonNode rootNode) {
        PaperRelationDto relationDto = new PaperRelationDto();
        
        // 解析references（引用的论文）
        JsonNode referencesNode = rootNode.get("references");
        if (referencesNode != null && referencesNode.isArray()) {
            List<PaperRelationDto.RelationPaper> references = new ArrayList<>();
            for (JsonNode refNode : referencesNode) {
                PaperRelationDto.RelationPaper paper = parseRelationPaper(refNode);
                if (paper != null) {
                    references.add(paper);
                }
            }
            relationDto.setReferences(references);
            log.info("解析到 {} 个引用论文", references.size());
        }
        
        // 解析citations（被引用的论文）
        JsonNode citationsNode = rootNode.get("citations");
        if (citationsNode != null && citationsNode.isArray()) {
            List<PaperRelationDto.RelationPaper> citations = new ArrayList<>();
            for (JsonNode citNode : citationsNode) {
                PaperRelationDto.RelationPaper paper = parseRelationPaper(citNode);
                if (paper != null) {
                    citations.add(paper);
                }
            }
            relationDto.setCitations(citations);
            log.info("解析到 {} 个被引论文", citations.size());
        }
        
        return relationDto;
    }
    
    /**
     * 解析单个论文的引用关系信息
     */
    private PaperRelationDto.RelationPaper parseRelationPaper(JsonNode paperNode) {
        try {
            PaperRelationDto.RelationPaper paper = new PaperRelationDto.RelationPaper();
            
            // 基本信息
            paper.setPaperId(getTextValue(paperNode, "paperId"));
            paper.setTitle(getTextValue(paperNode, "title"));
            paper.setDoi(getTextValue(paperNode, "doi"));
            paper.setVenue(getTextValue(paperNode, "venue"));
            paper.setAbstractText(getTextValue(paperNode, "abstract"));
            
            // 数值信息
            paper.setYear(getIntValue(paperNode, "year"));
            paper.setCitationCount(getIntValue(paperNode, "citationCount"));
            paper.setInfluentialCitationCount(getIntValue(paperNode, "influentialCitationCount"));
            
            // 作者信息
            JsonNode authorsNode = paperNode.get("authors");
            if (authorsNode != null && authorsNode.isArray()) {
                List<PaperRelationDto.Author> authors = new ArrayList<>();
                for (JsonNode authorNode : authorsNode) {
                    PaperRelationDto.Author author = new PaperRelationDto.Author();
                    author.setAuthorId(getTextValue(authorNode, "authorId"));
                    author.setName(getTextValue(authorNode, "name"));
                    authors.add(author);
                }
                paper.setAuthors(authors);
            }
            
            // 开放访问PDF
            JsonNode openAccessNode = paperNode.get("openAccessPdf");
            if (openAccessNode != null && !openAccessNode.isNull()) {
                PaperRelationDto.OpenAccessPdf openAccessPdf = new PaperRelationDto.OpenAccessPdf();
                openAccessPdf.setUrl(getTextValue(openAccessNode, "url"));
                openAccessPdf.setStatus(getTextValue(openAccessNode, "status"));
                paper.setOpenAccessPdf(openAccessPdf);
            }
            
            // 引用意图
            JsonNode intentNode = paperNode.get("intent");
            if (intentNode != null && intentNode.isArray()) {
                List<String> intents = new ArrayList<>();
                for (JsonNode intentItem : intentNode) {
                    intents.add(intentItem.asText());
                }
                paper.setIntent(intents);
            }
            
            return paper;
            
        } catch (Exception e) {
            log.error("解析论文信息失败", e);
            return null;
        }
    }
    
    /**
     * 安全获取文本值
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }
    
    /**
     * 安全获取整数值
     */
    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asInt() : null;
    }
}