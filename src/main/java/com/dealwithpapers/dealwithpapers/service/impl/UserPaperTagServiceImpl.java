package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.UserPaperTagDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserPaperTag;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.repository.UserPaperTagRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.UserPaperTagService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPaperTagServiceImpl implements UserPaperTagService {

    private final UserPaperTagRepository userPaperTagRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;

    @Autowired
    public UserPaperTagServiceImpl(
            UserPaperTagRepository userPaperTagRepository,
            UserRepository userRepository,
            PaperRepository paperRepository) {
        this.userPaperTagRepository = userPaperTagRepository;
        this.userRepository = userRepository;
        this.paperRepository = paperRepository;
    }

    @Override
    @Transactional
    public UserPaperTagDTO addTag(Long userId, Long paperId, String tagName, String tagColor) {
        // 检查用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 检查论文是否存在
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("论文不存在"));
        
        // 检查标签是否已存在
        Optional<UserPaperTag> existingTag = userPaperTagRepository
                .findByUserIdAndPaperIdAndTagName(userId, paperId, tagName);
        
        UserPaperTag tag;
        if (existingTag.isPresent()) {
            // 如果标签已存在，更新颜色
            tag = existingTag.get();
            tag.setTagColor(tagColor);
        } else {
            // 如果标签不存在，创建新标签
            tag = new UserPaperTag();
            tag.setUser(user);
            tag.setPaperId(paperId);
            tag.setTagName(tagName);
            tag.setTagColor(tagColor);
        }
        
        // 保存标签
        tag = userPaperTagRepository.save(tag);
        
        // 转换为DTO并返回
        UserPaperTagDTO dto = convertToDTO(tag);
        dto.setPaperTitle(paper.getTitle());
        dto.setPaperAuthors(String.join(", ", paper.getAuthors()));
        dto.setPaperYear(paper.getYear() != null ? paper.getYear().toString() : null);
        
        return dto;
    }

    @Override
    @Transactional
    public boolean removeTag(Long userId, Long paperId, String tagName) {
        try {
            // 检查标签是否存在
            Optional<UserPaperTag> existingTag = userPaperTagRepository
                    .findByUserIdAndPaperIdAndTagName(userId, paperId, tagName);
            
            if (existingTag.isPresent()) {
                userPaperTagRepository.deleteByUserIdAndPaperIdAndTagName(userId, paperId, tagName);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<UserPaperTagDTO> getTagsByPaper(Long userId, Long paperId) {
        List<UserPaperTag> tags = userPaperTagRepository.findByUserIdAndPaperId(userId, paperId);
        return tags.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public Map<String, List<UserPaperTagDTO>> getAllUserTags(Long userId) {
        List<UserPaperTag> allTags = userPaperTagRepository.findByUserId(userId);
        
        // 按标签名称分组
        Map<String, List<UserPaperTagDTO>> groupedTags = new HashMap<>();
        
        for (UserPaperTag tag : allTags) {
            UserPaperTagDTO dto = convertToDTO(tag);
            
            // 获取论文信息
            Paper paper = paperRepository.findById(tag.getPaperId()).orElse(null);
            if (paper != null) {
                dto.setPaperTitle(paper.getTitle());
                dto.setPaperAuthors(String.join(", ", paper.getAuthors()));
                dto.setPaperYear(paper.getYear() != null ? paper.getYear().toString() : null);
            }
            
            // 添加到分组中
            if (!groupedTags.containsKey(tag.getTagName())) {
                groupedTags.put(tag.getTagName(), new ArrayList<>());
            }
            groupedTags.get(tag.getTagName()).add(dto);
        }
        
        return groupedTags;
    }

    @Override
    public List<UserPaperTagDTO> getPapersByTag(Long userId, String tagName) {
        List<UserPaperTag> tags = userPaperTagRepository.findByUserIdAndTagName(userId, tagName);
        List<UserPaperTagDTO> results = new ArrayList<>();
        
        for (UserPaperTag tag : tags) {
            UserPaperTagDTO dto = convertToDTO(tag);
            
            // 获取论文信息
            Paper paper = paperRepository.findById(tag.getPaperId()).orElse(null);
            if (paper != null) {
                dto.setPaperTitle(paper.getTitle());
                dto.setPaperAuthors(String.join(", ", paper.getAuthors()));
                dto.setPaperYear(paper.getYear() != null ? paper.getYear().toString() : null);
                results.add(dto);
            }
        }
        
        return results;
    }

    @Override
    @Transactional
    public UserPaperTagDTO updateTagColor(Long userId, Long paperId, String tagName, String newColor) {
        // 检查标签是否存在
        UserPaperTag tag = userPaperTagRepository.findByUserIdAndPaperIdAndTagName(userId, paperId, tagName)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
        
        // 更新颜色
        tag.setTagColor(newColor);
        tag = userPaperTagRepository.save(tag);
        
        // 转换为DTO并返回
        UserPaperTagDTO dto = convertToDTO(tag);
        
        // 获取论文信息
        Paper paper = paperRepository.findById(paperId).orElse(null);
        if (paper != null) {
            dto.setPaperTitle(paper.getTitle());
            dto.setPaperAuthors(String.join(", ", paper.getAuthors()));
            dto.setPaperYear(paper.getYear() != null ? paper.getYear().toString() : null);
        }
        
        return dto;
    }
    
    /**
     * 将实体转换为DTO
     */
    private UserPaperTagDTO convertToDTO(UserPaperTag tag) {
        UserPaperTagDTO dto = new UserPaperTagDTO();
        dto.setId(tag.getId());
        dto.setUserId(tag.getUser().getId());
        dto.setPaperId(tag.getPaperId());
        dto.setTagName(tag.getTagName());
        dto.setTagColor(tag.getTagColor());
        dto.setCreatedAt(tag.getCreatedAt());
        return dto;
    }
} 