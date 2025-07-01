package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserFavorite;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.repository.UserFavoriteRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.UserFavoriteService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserFavoriteServiceImpl implements UserFavoriteService {

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaperRepository paperRepository;
    
    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long paperId) {
        System.out.println("=================== addFavorite服务方法开始 ===================");
        System.out.println("参数 - userId: " + userId + ", paperId: " + paperId);
        
        try {
            // 检查用户是否已经收藏过该论文
            boolean exists = userFavoriteRepository.existsByUserIdAndPaperId(userId, paperId);
            System.out.println("收藏记录是否已存在: " + exists);
            
            if (exists) {
                System.out.println("该论文已被收藏，不需要重复操作");
                return false; // 已收藏，不需要重复操作
            }
            
            // 获取用户实体
            Optional<User> userOpt = userRepository.findById(userId);
            System.out.println("用户查询结果: " + (userOpt.isPresent() ? "存在" : "不存在"));
            
            if (userOpt.isEmpty()) {
                System.out.println("用户不存在，userId: " + userId);
                return false; // 用户不存在
            }
            
            // 获取论文实体
            Optional<Paper> paperOpt = paperRepository.findById(paperId);
            System.out.println("论文查询结果: " + (paperOpt.isPresent() ? "存在" : "不存在"));
            
            Paper paper;
            
            // 如果论文不存在，创建一个新的论文记录
            if (paperOpt.isEmpty()) {
                System.out.println("论文不存在，将创建一个新的论文记录");
                
                paper = new Paper();
                paper.setId(paperId); // 使用请求中的ID
                paper.setTitle("论文 #" + paperId); // 设置临时标题
                paper.setYear(LocalDateTime.now().getYear()); // 使用当前年份
                paper.setCategory("未分类");
                paper.setAbstractText("这是自动创建的论文记录，用于存储收藏关系。");
                
                // 保存新论文
                paper = paperRepository.save(paper);
                System.out.println("成功创建新论文: " + paper.getId() + " - " + paper.getTitle());
            } else {
                paper = paperOpt.get();
            }
            
            User user = userOpt.get();
            
            // 创建新的收藏记录
            UserFavorite favorite = new UserFavorite();
            favorite.setUser(user);
            favorite.setPaper(paper);
            favorite.setCollectTime(LocalDateTime.now());
            
            // 保存收藏记录
            UserFavorite saved = userFavoriteRepository.save(favorite);
            System.out.println("收藏记录保存成功，ID: " + saved.getId());
            
            return true;
        } catch (Exception e) {
            // 捕获所有可能的异常
            System.err.println("收藏论文失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            System.out.println("=================== addFavorite服务方法结束 ===================");
        }
    }
    
    @Override
    @Transactional
    public boolean removeFavorite(Long userId, Long paperId) {
        System.out.println("=================== removeFavorite服务方法开始 ===================");
        System.out.println("参数 - userId: " + userId + ", paperId: " + paperId);
        
        try {
            // 检查收藏记录是否存在
            Optional<UserFavorite> favoriteOpt = userFavoriteRepository.findByUserIdAndPaperId(userId, paperId);
            System.out.println("收藏记录查询结果: " + (favoriteOpt.isPresent() ? "存在" : "不存在"));
            
            if (favoriteOpt.isEmpty()) {
                System.out.println("收藏记录不存在");
                return false; // 收藏记录不存在
            }
            
            // 删除收藏记录
            UserFavorite favorite = favoriteOpt.get();
            System.out.println("要删除的收藏记录ID: " + favorite.getId());
            
            userFavoriteRepository.delete(favorite);
            System.out.println("收藏记录删除成功");
            
            return true;
        } catch (Exception e) {
            // 捕获所有可能的异常
            System.err.println("取消收藏失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            System.out.println("=================== removeFavorite服务方法结束 ===================");
        }
    }
    
    @Override
    public Page<PaperDTO> getUserFavorites(Long userId, Pageable pageable) {
        System.out.println("获取用户收藏列表 - userId: " + userId);
        
        // 获取用户的收藏记录列表
        Page<UserFavorite> favoritesPage = userFavoriteRepository.findByUserId(userId, pageable);
        System.out.println("收藏记录数量: " + favoritesPage.getTotalElements());
        
        // 将收藏记录转换为PaperDTO
        Page<PaperDTO> result = favoritesPage.map(favorite -> {
            PaperDTO paperDTO = new PaperDTO();
            Paper paper = favorite.getPaper();
            BeanUtils.copyProperties(paper, paperDTO);
            return paperDTO;
        });
        
        return result;
    }
    
    @Override
    public boolean checkIsFavorite(Long userId, Long paperId) {
        System.out.println("=================== checkIsFavorite服务方法开始 ===================");
        System.out.println("参数 - userId: " + userId + ", paperId: " + paperId);
        
        try {
            boolean isFavorite = userFavoriteRepository.existsByUserIdAndPaperId(userId, paperId);
            System.out.println("收藏状态查询结果: " + (isFavorite ? "已收藏" : "未收藏"));
            return isFavorite;
        } catch (Exception e) {
            // 捕获所有可能的异常
            System.err.println("检查收藏状态失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            System.out.println("=================== checkIsFavorite服务方法结束 ===================");
        }
    }
} 