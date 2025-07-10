package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperRelationDto;
import com.dealwithpapers.dealwithpapers.entity.PaperRelation;
import com.dealwithpapers.dealwithpapers.service.PaperRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paper-relations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaperRelationController {
    
    private final PaperRelationService paperRelationService;
    
    /**
     * 保存论文引用关系
     */
    @PostMapping("/save")
    public ResponseEntity<?> savePaperRelations(@RequestBody PaperRelationDto relationDto) {
        try {
            paperRelationService.savePaperRelations(relationDto);
            return ResponseEntity.ok().body(new ApiResponse(true, "论文引用关系保存成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "保存失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取论文的引用关系
     */
    @GetMapping("/{paperId}/references")
    public ResponseEntity<?> getPaperReferences(@PathVariable Long paperId) {
        try {
            List<PaperRelation> references = paperRelationService.getPaperReferences(paperId);
            return ResponseEntity.ok().body(new ApiResponse(true, "获取成功", references));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取论文的被引用关系
     */
    @GetMapping("/{paperId}/citations")
    public ResponseEntity<?> getPaperCitations(@PathVariable Long paperId) {
        try {
            List<PaperRelation> citations = paperRelationService.getPaperCitations(paperId);
            return ResponseEntity.ok().body(new ApiResponse(true, "获取成功", citations));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取论文的所有关系（引用和被引用）
     */
    @GetMapping("/{paperId}/all")
    public ResponseEntity<?> getAllPaperRelations(@PathVariable Long paperId) {
        try {
            List<PaperRelation> references = paperRelationService.getPaperReferences(paperId);
            List<PaperRelation> citations = paperRelationService.getPaperCitations(paperId);
            
            return ResponseEntity.ok().body(new ApiResponse(true, "获取成功", new RelationResponse(references, citations)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除论文的所有关系
     */
    @DeleteMapping("/{paperId}")
    public ResponseEntity<?> deletePaperRelations(@PathVariable Long paperId) {
        try {
            paperRelationService.deletePaperRelations(paperId);
            return ResponseEntity.ok().body(new ApiResponse(true, "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "删除失败: " + e.getMessage()));
        }
    }
    
    // 内部类：API响应
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
    
    // 内部类：关系响应
    public static class RelationResponse {
        private List<PaperRelation> references;
        private List<PaperRelation> citations;
        
        public RelationResponse(List<PaperRelation> references, List<PaperRelation> citations) {
            this.references = references;
            this.citations = citations;
        }
        
        // Getters
        public List<PaperRelation> getReferences() { return references; }
        public List<PaperRelation> getCitations() { return citations; }
    }
}