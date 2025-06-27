package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.UserSearchHistoryDTO;
import com.dealwithpapers.dealwithpapers.dto.UserViewHistoryDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserSearchHistory;
import com.dealwithpapers.dealwithpapers.entity.UserViewHistory;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.repository.UserSearchHistoryRepository;
import com.dealwithpapers.dealwithpapers.repository.UserViewHistoryRepository;
import com.dealwithpapers.dealwithpapers.service.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserHistoryServiceImpl implements UserHistoryService {

    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserViewHistoryRepository viewHistoryRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;

    // ===== 搜索历史相关方法实现 =====

    @Override
    @Transactional
    public UserSearchHistoryDTO saveSearchHistory(Long userId, String searchText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserSearchHistory searchHistory = new UserSearchHistory();
        searchHistory.setUser(user);
        searchHistory.setSearchText(searchText);
        searchHistory.setSearchTime(LocalDateTime.now());

        UserSearchHistory savedHistory = searchHistoryRepository.save(searchHistory);
        
        // 限制用户搜索历史记录数量
        limitUserSearchHistory(userId, 50);
        
        return convertToSearchHistoryDTO(savedHistory);
    }

    @Override
    public Page<UserSearchHistoryDTO> getUserSearchHistory(Long userId, Pageable pageable) {
        Page<UserSearchHistory> historyPage = searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId, pageable);
        
        List<UserSearchHistoryDTO> dtoList = historyPage.getContent().stream()
                .map(this::convertToSearchHistoryDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, historyPage.getTotalElements());
    }

    @Override
    public List<UserSearchHistoryDTO> getRecentSearchHistory(Long userId, int limit) {
        if (limit <= 0) {
            limit = 10; // 默认值
        }
        
        // 限制最大值为50
        limit = Math.min(limit, 50);
        
        List<UserSearchHistory> histories = limit == 10 
                ? searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(userId)
                : searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId).stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        
        return histories.stream()
                .map(this::convertToSearchHistoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDistinctSearchTerms(Long userId, int limit) {
        // 限制最大值为50
        limit = Math.min(limit, 50);
        return searchHistoryRepository.findDistinctSearchTextByUserId(userId, limit);
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long id) {
        if (!searchHistoryRepository.existsById(id)) {
            throw new RuntimeException("搜索记录不存在");
        }
        searchHistoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void clearUserSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
    
    @Override
    @Transactional
    public void limitUserSearchHistory(Long userId, int maxRecords) {
        // 获取用户的搜索历史记录总数
        List<UserSearchHistory> allHistories = searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId);
        
        // 如果超过最大记录数，删除旧记录
        if (allHistories.size() > maxRecords) {
            // 获取需要删除的记录
            List<UserSearchHistory> recordsToDelete = allHistories.subList(maxRecords, allHistories.size());
            
            // 删除这些记录
            for (UserSearchHistory record : recordsToDelete) {
                searchHistoryRepository.deleteById(record.getId());
            }
        }
    }

    // ===== 浏览历史相关方法实现 =====

    @Override
    @Transactional
    public UserViewHistoryDTO saveViewHistory(Long userId, String paperId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        UserViewHistory viewHistory = new UserViewHistory();
        viewHistory.setUser(user);
        viewHistory.setPaper(paper);
        viewHistory.setViewTime(LocalDateTime.now());

        UserViewHistory savedHistory = viewHistoryRepository.save(viewHistory);
        
        // 限制用户浏览历史记录数量
        limitUserViewHistory(userId, 50);
        
        return convertToViewHistoryDTO(savedHistory);
    }

    @Override
    public Page<UserViewHistoryDTO> getUserViewHistory(Long userId, Pageable pageable) {
        Page<UserViewHistory> historyPage = viewHistoryRepository.findByUserIdOrderByViewTimeDesc(userId, pageable);
        
        List<UserViewHistoryDTO> dtoList = historyPage.getContent().stream()
                .map(this::convertToViewHistoryDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, historyPage.getTotalElements());
    }

    @Override
    public List<UserViewHistoryDTO> getRecentViewHistory(Long userId, int limit) {
        if (limit <= 0) {
            limit = 10; // 默认值
        }
        
        // 限制最大值为50
        limit = Math.min(limit, 50);
        
        List<UserViewHistory> histories = limit == 10 
                ? viewHistoryRepository.findTop10ByUserIdOrderByViewTimeDesc(userId)
                : viewHistoryRepository.findByUserIdOrderByViewTimeDesc(userId).stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        
        return histories.stream()
                .map(this::convertToViewHistoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getPaperViewCount(String paperId) {
        return viewHistoryRepository.countByPaperId(paperId);
    }

    @Override
    @Transactional
    public void deleteViewHistory(Long id) {
        if (!viewHistoryRepository.existsById(id)) {
            throw new RuntimeException("浏览记录不存在");
        }
        viewHistoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void clearUserViewHistory(Long userId) {
        viewHistoryRepository.deleteByUserId(userId);
    }
    
    @Override
    @Transactional
    public void limitUserViewHistory(Long userId, int maxRecords) {
        // 获取用户的浏览历史记录总数
        List<UserViewHistory> allHistories = viewHistoryRepository.findByUserIdOrderByViewTimeDesc(userId);
        
        // 如果超过最大记录数，删除旧记录
        if (allHistories.size() > maxRecords) {
            // 获取需要删除的记录
            List<UserViewHistory> recordsToDelete = allHistories.subList(maxRecords, allHistories.size());
            
            // 删除这些记录
            for (UserViewHistory record : recordsToDelete) {
                viewHistoryRepository.deleteById(record.getId());
            }
        }
    }

    // ===== 辅助方法 =====

    private UserSearchHistoryDTO convertToSearchHistoryDTO(UserSearchHistory history) {
        return new UserSearchHistoryDTO(
                history.getId(),
                history.getUser().getId(),
                history.getSearchText(),
                history.getSearchTime()
        );
    }

    private UserViewHistoryDTO convertToViewHistoryDTO(UserViewHistory history) {
        return new UserViewHistoryDTO(
                history.getId(),
                history.getUser().getId(),
                history.getPaper().getId(),
                history.getPaper().getTitle(),
                history.getViewTime()
        );
    }
    
    private PaperDTO convertToPaperDTO(Paper paper) {
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