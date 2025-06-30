package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
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
} 