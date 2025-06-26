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
    public PaperDTO getPaperById(String id) {
        return paperRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("论文未找到，ID: " + id));
    }

    @Override
    public List<PaperDTO> getAllPapers() {
        return paperRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperDTO updatePaper(String id, PaperDTO paperDTO) {
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
    public void deletePaper(String id) {
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
            return getAllPapers();
        }
        
        searchTerm = searchTerm.trim();
        Set<Paper> results = new HashSet<>();
        
        // 首先尝试使用综合搜索
        results.addAll(paperRepository.searchByTerm(searchTerm));
        
        // 尝试解析年份
        try {
            int year = Integer.parseInt(searchTerm);
            results.addAll(paperRepository.findByYear(year));
        } catch (NumberFormatException e) {
            // 不是有效的年份，忽略
        }
        
        return results.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // 辅助方法：将DTO转换为实体
    private Paper convertToEntity(PaperDTO dto) {
        Paper paper = new Paper();
        paper.setId(dto.getId());
        paper.setTitle(dto.getTitle());
        paper.setAuthors(dto.getAuthors());
        paper.setAbstractText(dto.getAbstractText());
        paper.setYear(dto.getYear());
        paper.setJournal(dto.getJournal());
        paper.setCategory(dto.getCategory());
        paper.setUrl(dto.getUrl());
        return paper;
    }
    
    // 辅助方法：将实体转换为DTO
    private PaperDTO convertToDTO(Paper paper) {
        return new PaperDTO(
                paper.getId(),
                paper.getTitle(),
                paper.getAuthors(),
                paper.getAbstractText(),
                paper.getYear(),
                paper.getJournal(),
                paper.getCategory(),
                paper.getUrl()
        );
    }
} 