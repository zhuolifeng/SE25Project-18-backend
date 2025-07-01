package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;

import java.util.List;

public interface PaperService {
    
    // 保存论文信息
    PaperDTO savePaper(PaperDTO paperDTO);
    
    // 根据ID获取论文
    PaperDTO getPaperById(Long id);
    
    // 根据DOI获取论文
    PaperDTO getPaperByDoi(String doi);
    
    // 获取所有论文
    List<PaperDTO> getAllPapers();
    
    // 更新论文信息
    PaperDTO updatePaper(Long id, PaperDTO paperDTO);
    
    // 删除论文
    void deletePaper(Long id);
    
    // 搜索论文（ID、标题或作者）
    List<PaperDTO> searchPapers(PaperSearchDTO searchDTO);
    
    // 通过搜索词搜索论文（ID、标题或作者）
    List<PaperDTO> searchByTerm(String searchTerm);
    
    // 检查论文ID是否存在
    boolean existsById(Long id);
    
    // 通过DOI查找论文实体
    Paper findByDoi(String doi);
} 