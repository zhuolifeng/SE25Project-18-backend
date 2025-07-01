package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @PostMapping
    public ResponseEntity<?> savePaper(@RequestBody PaperDTO paperDTO) {
        try {
            PaperDTO savedPaper = paperService.savePaper(paperDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPaper);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaperById(@PathVariable Long id) {
        try {
            PaperDTO paper = paperService.getPaperById(id);
            return ResponseEntity.ok(paper);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping("/doi/{doi}")
    public ResponseEntity<?> getPaperByDoi(@PathVariable String doi) {
        try {
            PaperDTO paper = paperService.getPaperByDoi(doi);
            return ResponseEntity.ok(paper);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<PaperDTO>> getAllPapers() {
        List<PaperDTO> papers = paperService.getAllPapers();
        return ResponseEntity.ok(papers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePaper(@PathVariable Long id, @RequestBody PaperDTO paperDTO) {
        try {
            PaperDTO updatedPaper = paperService.updatePaper(id, paperDTO);
            return ResponseEntity.ok(updatedPaper);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable Long id) {
        try {
            paperService.deletePaper(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "论文删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PaperDTO>> searchPapers(@RequestParam(required = false) String searchTerm, 
                                                      @RequestParam(required = false) Integer year) {
        PaperSearchDTO searchDTO = new PaperSearchDTO();
        searchDTO.setSearchTerm(searchTerm);
        searchDTO.setYear(year);
        
        List<PaperDTO> results = paperService.searchPapers(searchDTO);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<PaperDTO>> searchPapersPost(@RequestBody(required = false) PaperSearchDTO searchDTO) {
        if (searchDTO == null) {
            searchDTO = new PaperSearchDTO();
        }
        
        List<PaperDTO> results = paperService.searchPapers(searchDTO);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/search/{term}")
    public ResponseEntity<List<PaperDTO>> searchByTerm(@PathVariable String term) {
        List<PaperDTO> results = paperService.searchByTerm(term);
        return ResponseEntity.ok(results);
    }

    /**
     * 检查论文是否存在于数据库中
     * @param id 论文ID或者其他标识符（如DOI）
     * @return 论文存在性信息
     */
    @GetMapping("/check/{id}")
    public ResponseEntity<Map<String, Object>> checkPaperExists(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        // 记录检查开始
        System.out.println("检查论文是否存在: " + id);
        
        try {
            // 尝试作为数字ID处理
            Long numericId = null;
            try {
                numericId = Long.valueOf(id);
                System.out.println("转换为数字ID: " + numericId);
            } catch (NumberFormatException e) {
                System.out.println("非数字ID，将尝试作为DOI处理");
            }
            
            // 尝试通过数字ID查找
            if (numericId != null) {
                boolean exists = paperService.existsById(numericId);
                System.out.println("通过数字ID查找结果: " + exists);
                
                response.put("exists", exists);
                response.put("dbId", numericId);
                
                if (exists) {
                    return ResponseEntity.ok(response);
                }
            }
            
            // 尝试通过DOI查找
            Paper paper = paperService.findByDoi(id);
            if (paper != null) {
                System.out.println("通过DOI找到论文: " + paper.getId());
                response.put("exists", true);
                response.put("dbId", paper.getId());
                return ResponseEntity.ok(response);
            }
            
            // 如果都找不到，返回不存在
            System.out.println("论文不存在: " + id);
            response.put("exists", false);
            response.put("message", "论文不存在");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("检查论文存在性时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("exists", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 