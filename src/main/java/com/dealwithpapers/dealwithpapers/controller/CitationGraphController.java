package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperRelationDto;
import com.dealwithpapers.dealwithpapers.entity.PaperRelation;
import com.dealwithpapers.dealwithpapers.service.CitationDataService;
import com.dealwithpapers.dealwithpapers.service.PaperRelationService;
import com.dealwithpapers.dealwithpapers.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 引用图控制器
 * 提供引用图数据的缓存API端点
 */
@RestController
@RequestMapping("/api/citation-graph")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CitationGraphController {
    
    private final PaperService paperService;
    private final PaperRelationService paperRelationService;
    private final CitationDataService citationDataService;
    
    /**
     * 获取论文的引用图数据
     * @param paperId 论文ID
     * @return 引用图数据（D3.js格式）
     */
    @GetMapping("/{paperId}")
    public ResponseEntity<?> getCitationGraphData(@PathVariable Long paperId) {
        try {
            log.info("获取论文引用图数据，论文ID: {}", paperId);
            
            // 检查论文是否存在
            if (!paperService.existsById(paperId)) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "论文不存在"));
            }
            
            // 获取中心论文信息
            PaperDTO centerPaper = paperService.getPaperById(paperId);
            
            // 优先从数据库获取缓存的引用数据
            List<PaperRelation> references = paperRelationService.getPaperReferences(paperId);
            List<PaperRelation> citations = paperRelationService.getPaperCitations(paperId);
            
            // 如果数据库中没有引用数据，尝试从API获取
            if (references.isEmpty() && citations.isEmpty()) {
                log.info("数据库中无引用数据，尝试从API获取，论文ID: {}", paperId);
                
                PaperRelationDto relationDto = null;
                
                // 优先使用DOI获取
                if (centerPaper.getDoi() != null && !centerPaper.getDoi().trim().isEmpty()) {
                    relationDto = citationDataService.getCitationDataByDoi(centerPaper.getDoi());
                }
                
                // 如果DOI获取失败，尝试用标题获取
                if (relationDto == null && centerPaper.getTitle() != null && !centerPaper.getTitle().trim().isEmpty()) {
                    relationDto = citationDataService.getCitationDataByTitle(centerPaper.getTitle());
                }
                
                // 如果成功获取到引用数据，保存到数据库并重新查询
                if (relationDto != null) {
                    relationDto.setPaperId(paperId);
                    paperRelationService.savePaperRelations(relationDto);
                    
                    // 重新从数据库获取
                    references = paperRelationService.getPaperReferences(paperId);
                    citations = paperRelationService.getPaperCitations(paperId);
                }
            }
            
            // 构建D3.js图数据格式
            Map<String, Object> graphData = buildGraphData(centerPaper, references, citations);
            
            return ResponseEntity.ok().body(new ApiResponse(true, "获取成功", graphData));
            
        } catch (Exception e) {
            log.error("获取引用图数据失败，论文ID: {}", paperId, e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 强制刷新论文的引用数据
     * @param paperId 论文ID
     * @return 刷新结果
     */
    @PostMapping("/{paperId}/refresh")
    public ResponseEntity<?> refreshCitationData(@PathVariable Long paperId) {
        try {
            log.info("强制刷新论文引用数据，论文ID: {}", paperId);
            
            // 检查论文是否存在
            if (!paperService.existsById(paperId)) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "论文不存在"));
            }
            
            // 获取中心论文信息
            PaperDTO centerPaper = paperService.getPaperById(paperId);
            
            // 删除现有的引用数据
            paperRelationService.deletePaperRelations(paperId);
            
            // 从API重新获取
            PaperRelationDto relationDto = null;
            
            // 优先使用DOI获取
            if (centerPaper.getDoi() != null && !centerPaper.getDoi().trim().isEmpty()) {
                relationDto = citationDataService.getCitationDataByDoi(centerPaper.getDoi());
            }
            
            // 如果DOI获取失败，尝试用标题获取
            if (relationDto == null && centerPaper.getTitle() != null && !centerPaper.getTitle().trim().isEmpty()) {
                relationDto = citationDataService.getCitationDataByTitle(centerPaper.getTitle());
            }
            
            // 保存新的引用数据
            if (relationDto != null) {
                relationDto.setPaperId(paperId);
                paperRelationService.savePaperRelations(relationDto);
                
                int refCount = relationDto.getReferences() != null ? relationDto.getReferences().size() : 0;
                int citCount = relationDto.getCitations() != null ? relationDto.getCitations().size() : 0;
                
                return ResponseEntity.ok().body(new ApiResponse(true, 
                    String.format("刷新成功，获取到 %d 个引用和 %d 个被引用", refCount, citCount)));
            } else {
                return ResponseEntity.ok().body(new ApiResponse(true, "未找到引用数据"));
            }
            
        } catch (Exception e) {
            log.error("刷新引用数据失败，论文ID: {}", paperId, e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, "刷新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 构建D3.js图数据格式
     */
    private Map<String, Object> buildGraphData(PaperDTO centerPaper, List<PaperRelation> references, List<PaperRelation> citations) {
        Map<String, Object> graphData = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        
        // 创建中心节点
        Map<String, Object> centerNode = new HashMap<>();
        centerNode.put("id", "center_" + centerPaper.getId());
        centerNode.put("type", "center");
        centerNode.put("title", centerPaper.getTitle());
        centerNode.put("label", centerPaper.getTitle());
        centerNode.put("authors", centerPaper.getAuthors());
        centerNode.put("year", centerPaper.getYear());
        centerNode.put("doi", centerPaper.getDoi());
        centerNode.put("venue", centerPaper.getJournal());
        centerNode.put("citationCount", 0); // 中心节点的引用数可以从其他地方获取
        centerNode.put("size", calculateNodeSize(0, true, centerPaper.getYear()));
        nodes.add(centerNode);
        
        // 创建引用节点（蓝色）
        for (PaperRelation ref : references) {
            Map<String, Object> node = createNodeFromRelation(ref, "reference");
            nodes.add(node);
            
            // 创建连接
            Map<String, Object> link = new HashMap<>();
            link.put("source", "center_" + centerPaper.getId());
            link.put("target", ref.getId().toString());
            link.put("type", "references");
            link.put("color", "#1890ff");
            links.add(link);
        }
        
        // 创建被引节点（绿色）
        for (PaperRelation cit : citations) {
            Map<String, Object> node = createNodeFromRelation(cit, "citation");
            nodes.add(node);
            
            // 创建连接
            Map<String, Object> link = new HashMap<>();
            link.put("source", cit.getId().toString());
            link.put("target", "center_" + centerPaper.getId());
            link.put("type", "citations");
            link.put("color", "#52c41a");
            links.add(link);
        }
        
        graphData.put("nodes", nodes);
        graphData.put("links", links);
        graphData.put("centralNode", centerPaper);
        
        log.info("构建图数据完成: {} 个节点, {} 个连接", nodes.size(), links.size());
        
        return graphData;
    }
    
    /**
     * 从PaperRelation创建节点
     */
    private Map<String, Object> createNodeFromRelation(PaperRelation relation, String type) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", relation.getId().toString());
        node.put("type", type);
        node.put("title", relation.getTargetTitle());
        node.put("label", relation.getTargetTitle());
        node.put("authors", relation.getTargetAuthors());
        node.put("year", relation.getTargetYear());
        node.put("doi", relation.getTargetDoi());
        node.put("venue", relation.getTargetVenue());
        node.put("citationCount", relation.getCitationCount());
        node.put("url", relation.getOpenAccessUrl());
        node.put("size", calculateNodeSize(relation.getCitationCount(), false, relation.getTargetYear()));
        return node;
    }
    
    /**
     * 计算节点大小（复用前端逻辑）
     */
    private int calculateNodeSize(Integer citationCount, boolean isCenter, Integer nodeYear) {
        if (isCenter) {
            return 45; // 中心节点稍大一些
        }
        
        int minSize = 12;
        int maxSize = 70;
        int currentYear = 2024; // 可以改为动态获取当前年份
        int yearsOld = nodeYear != null ? Math.max(1, currentYear - nodeYear) : 1;
        int maxCitations = Math.min(50000, 100 * yearsOld);
        
        if (citationCount == null || citationCount <= 0) {
            return minSize;
        }
        
        // 使用平方根缩放来避免节点过大
        double normalizedCount = Math.min(1.0, (double) citationCount / maxCitations);
        double sqrtNormalized = Math.sqrt(normalizedCount);
        
        return (int) (minSize + (maxSize - minSize) * sqrtNormalized);
    }
    
    /**
     * API响应类
     */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
        
        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
}