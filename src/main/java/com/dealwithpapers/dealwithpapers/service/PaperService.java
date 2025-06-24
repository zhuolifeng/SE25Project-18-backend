package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;

import java.util.List;

public interface PaperService {
    
    // 保存论文信息
    PaperDTO savePaper(PaperDTO paperDTO);
    
    // 根据ID获取论文
    PaperDTO getPaperById(String id);
    
    // 获取所有论文
    List<PaperDTO> getAllPapers();
    
    // 更新论文信息
    PaperDTO updatePaper(String id, PaperDTO paperDTO);
    
    // 删除论文
    void deletePaper(String id);
    
    // 搜索论文（ID、标题或作者）
    List<PaperDTO> searchPapers(PaperSearchDTO searchDTO);
    
    // 通过搜索词搜索论文（ID、标题或作者）
    List<PaperDTO> searchByTerm(String searchTerm);
} 