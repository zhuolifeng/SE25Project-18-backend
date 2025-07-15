package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    
    @Override
    @Transactional
    public PaperDTO savePaper(PaperDTO paperDTO) {
        Paper paper = convertToEntity(paperDTO);
        Paper savedPaper = paperRepository.save(paper);
        return convertToDTO(savedPaper);
    }

    @Override
    public PaperDTO getPaperById(Long id) {
        return paperRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("论文未找到，ID: " + id));
    }
    
    @Override
    public PaperDTO getPaperByDoi(String doi) {
        if (doi == null || doi.trim().isEmpty()) {
            throw new IllegalArgumentException("DOI不能为空");
        }
        
        Paper paper = paperRepository.findByDoi(doi);
        if (paper == null) {
            throw new RuntimeException("论文未找到，DOI: " + doi);
        }
        
        return convertToDTO(paper);
    }

    @Override
    public List<PaperDTO> getAllPapers() {
        return paperRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperDTO updatePaper(Long id, PaperDTO paperDTO) {
        if (!paperRepository.existsById(id)) {
            throw new RuntimeException("论文未找到，ID: " + id);
        }
        
        Paper paper = convertToEntity(paperDTO);
        paper.setId(id); // 确保ID一致
        Paper updatedPaper = paperRepository.save(paper);
        return convertToDTO(updatedPaper);
    }

    @Override
    @Transactional
    public void deletePaper(Long id) {
        if (!paperRepository.existsById(id)) {
            throw new RuntimeException("论文未找到，ID: " + id);
        }
        paperRepository.deleteById(id);
    }

    @Override
    public List<PaperDTO> searchPapers(PaperSearchDTO searchDTO) {
        String searchTerm = searchDTO.getSearchTerm();
        Integer year = searchDTO.getYear();
        
        // 如果没有提供搜索条件，返回所有论文
        if ((searchTerm == null || searchTerm.trim().isEmpty()) && year == null) {
            return getAllPapers();
        }
        
        // 如果只有年份筛选
        if ((searchTerm == null || searchTerm.trim().isEmpty()) && year != null) {
            return paperRepository.findByYear(year).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        
        // 如果只有搜索词
        if (searchTerm != null && !searchTerm.trim().isEmpty() && year == null) {
            return searchByTerm(searchTerm);
        }
        
        // 如果同时有搜索词和年份筛选
        Set<Paper> results = new HashSet<>(paperRepository.searchByTerm(searchTerm.trim()));
        
        // 对结果进行年份筛选
        return results.stream()
                .filter(paper -> paper.getYear() != null && 
                        paper.getYear().equals(year))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaperDTO> searchByTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return paperRepository.searchByTerm(searchTerm.trim()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // 实体转换为DTO
    private PaperDTO convertToDTO(Paper paper) {
        PaperDTO dto = new PaperDTO();
        dto.setId(paper.getId());
        dto.setDoi(paper.getDoi());
        dto.setTitle(paper.getTitle());
        dto.setAuthors(paper.getAuthors());
        dto.setAbstractText(paper.getAbstractText());
        dto.setHasAbstract(paper.getHasAbstract());
        dto.setYear(paper.getYear());
        dto.setJournal(paper.getJournal());
        dto.setCategory(paper.getCategory());
        dto.setUrl(paper.getUrl());
        return dto;
    }
    
    // DTO转换为实体
    private Paper convertToEntity(PaperDTO dto) {
        Paper paper = new Paper();
        paper.setId(dto.getId()); // 可能为null，如果是新建
        paper.setDoi(dto.getDoi());
        paper.setTitle(dto.getTitle());
        paper.setAuthors(dto.getAuthors());
        paper.setAbstractText(dto.getAbstractText());
        paper.setHasAbstract(dto.getHasAbstract() != null ? dto.getHasAbstract() : false);
        
        // 移除根据abstractText自动设置hasAbstract的逻辑
        
        paper.setYear(dto.getYear());
        paper.setJournal(dto.getJournal());
        paper.setCategory(dto.getCategory());
        paper.setUrl(dto.getUrl());
        return paper;
    }
    @Override
    public boolean existsById(Long id) {
        return paperRepository.existsById(id);
    }
    
    @Override
    public Paper findByDoi(String doi) {
        if (doi == null || doi.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 先尝试精确匹配
            Paper paper = paperRepository.findByDoi(doi);
            if (paper != null) {
                return paper;
            }
            
            // 如果精确匹配不到，尝试通过搜索找到匹配的DOI
            String cleanedDoi = doi.trim().toLowerCase().replace("doi:", "");
            List<Paper> papers = paperRepository.searchByTerm(cleanedDoi);
            
            // 查找完全匹配DOI的论文
            for (Paper p : papers) {
                if (p.getDoi() != null && 
                    (p.getDoi().equalsIgnoreCase(cleanedDoi) || 
                     p.getDoi().equalsIgnoreCase("doi:" + cleanedDoi))) {
                    return p;
                }
            }
            
            return null;
        } catch (Exception e) {
            // 出现异常时记录并返回null
            System.err.println("查找DOI发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 