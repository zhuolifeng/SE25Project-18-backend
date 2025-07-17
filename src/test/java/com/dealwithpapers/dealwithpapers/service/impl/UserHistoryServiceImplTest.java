package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.UserViewHistoryDTO;
import com.dealwithpapers.dealwithpapers.entity.Paper;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.entity.UserViewHistory;
import com.dealwithpapers.dealwithpapers.repository.PaperRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.repository.UserViewHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserHistoryServiceImplTest {

    @Mock
    private UserViewHistoryRepository viewHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaperRepository paperRepository;

    @InjectMocks
    private UserHistoryServiceImpl userHistoryService;

    private User testUser;
    private Paper testPaper;
    private UserViewHistory testViewHistory;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testPaper = new Paper();
        testPaper.setId(1L);
        testPaper.setTitle("Test Paper");

        testViewHistory = new UserViewHistory();
        testViewHistory.setId(1L);
        testViewHistory.setUser(testUser);
        testViewHistory.setPaper(testPaper);
        testViewHistory.setViewTime(now);
    }

    @Test
    @DisplayName("保存浏览历史 - 新记录")
    void saveViewHistory_NewRecord_ShouldSaveAndReturnDTO() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paperRepository.findById(1L)).thenReturn(Optional.of(testPaper));
        when(viewHistoryRepository.findRecentView(eq(1L), eq(1L), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(viewHistoryRepository.save(any(UserViewHistory.class))).thenReturn(testViewHistory);

        // Act
        UserViewHistoryDTO result = userHistoryService.saveViewHistory(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getPaperId());
        assertEquals("Test Paper", result.getPaperTitle());

        verify(viewHistoryRepository).save(any(UserViewHistory.class));
    }

    @Test
    @DisplayName("保存浏览历史 - 更新已有记录")
    void saveViewHistory_WithExistingRecentView_ShouldUpdateViewTime() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paperRepository.findById(1L)).thenReturn(Optional.of(testPaper));
        when(viewHistoryRepository.findRecentView(eq(1L), eq(1L), any(LocalDateTime.class)))
            .thenReturn(Optional.of(testViewHistory));
        when(viewHistoryRepository.save(any(UserViewHistory.class))).thenReturn(testViewHistory);

        // Act
        UserViewHistoryDTO result = userHistoryService.saveViewHistory(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(viewHistoryRepository).save(testViewHistory);
    }

    @Test
    @DisplayName("用户不存在时保存浏览历史")
    void saveViewHistory_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userHistoryService.saveViewHistory(999L, 1L));
    }

    @Test
    @DisplayName("论文不存在时保存浏览历史")
    void saveViewHistory_PaperNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paperRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userHistoryService.saveViewHistory(1L, 999L));
    }

    @Test
    @DisplayName("获取分页浏览历史")
    void getUserViewHistory_ShouldReturnPagedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserViewHistory> historyPage = new PageImpl<>(
            Arrays.asList(testViewHistory),
            pageable,
            1
        );

        when(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(1L, pageable))
            .thenReturn(historyPage);

        // Act
        Page<UserViewHistoryDTO> result = userHistoryService.getUserViewHistory(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("获取最近浏览历史 - 默认10条")
    void getRecentViewHistory_Default_ShouldReturnTop10() {
        // Arrange
        List<UserViewHistory> histories = Arrays.asList(testViewHistory);

        when(viewHistoryRepository.findTop10ByUserIdOrderByViewTimeDesc(1L))
            .thenReturn(histories);

        // Act
        List<UserViewHistoryDTO> result = userHistoryService.getRecentViewHistory(1L, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("获取最近浏览历史 - 自定义条数")
    void getRecentViewHistory_CustomLimit_ShouldReturnLimitedResults() {
        // Arrange
        List<UserViewHistory> histories = Arrays.asList(testViewHistory);

        when(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(1L))
            .thenReturn(histories);

        // Act
        List<UserViewHistoryDTO> result = userHistoryService.getRecentViewHistory(1L, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取最近浏览历史 - 负数限制")
    void getRecentViewHistory_NegativeLimit_ShouldUseDefaultLimit() {
        // Arrange
        List<UserViewHistory> histories = Arrays.asList(testViewHistory);

        when(viewHistoryRepository.findTop10ByUserIdOrderByViewTimeDesc(1L))
            .thenReturn(histories);

        // Act
        List<UserViewHistoryDTO> result = userHistoryService.getRecentViewHistory(1L, -5);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(viewHistoryRepository).findTop10ByUserIdOrderByViewTimeDesc(1L);
    }

    @Test
    @DisplayName("获取最近浏览历史 - 超过最大限制")
    void getRecentViewHistory_ExceedMaxLimit_ShouldCapLimit() {
        // Arrange
        List<UserViewHistory> histories = Arrays.asList(testViewHistory);

        when(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(1L))
            .thenReturn(histories);

        // Act
        List<UserViewHistoryDTO> result = userHistoryService.getRecentViewHistory(1L, 100);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // 验证限制被设置为最大值50
        verify(viewHistoryRepository).findByUserIdOrderByViewTimeDesc(1L);
    }

    @Test
    @DisplayName("获取论文浏览次数")
    void getPaperViewCount_ShouldReturnCorrectCount() {
        // Arrange
        when(viewHistoryRepository.countByPaperId(1L)).thenReturn(5L);

        // Act
        long count = userHistoryService.getPaperViewCount(1L);

        // Assert
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("删除浏览记录")
    void deleteViewHistory_ShouldDeleteRecord() {
        // Arrange
        when(viewHistoryRepository.existsById(1L)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> userHistoryService.deleteViewHistory(1L));

        // Assert
        verify(viewHistoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的浏览记录")
    void deleteViewHistory_NonExistent_ShouldThrowException() {
        // Arrange
        when(viewHistoryRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userHistoryService.deleteViewHistory(999L));
    }

    @Test
    @DisplayName("清空用户浏览历史")
    void clearUserViewHistory_ShouldDeleteAllUserRecords() {
        // Act
        userHistoryService.clearUserViewHistory(1L);

        // Assert
        verify(viewHistoryRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("限制用户浏览历史数量 - 需要删除")
    void limitUserViewHistory_ExceedsLimit_ShouldDeleteOldRecords() {
        // Arrange
        UserViewHistory oldHistory1 = new UserViewHistory();
        oldHistory1.setId(2L);
        oldHistory1.setUser(testUser);
        oldHistory1.setPaper(testPaper);
        oldHistory1.setViewTime(now.minusDays(2));

        UserViewHistory oldHistory2 = new UserViewHistory();
        oldHistory2.setId(3L);
        oldHistory2.setUser(testUser);
        oldHistory2.setPaper(testPaper);
        oldHistory2.setViewTime(now.minusDays(3));

        List<UserViewHistory> allHistories = Arrays.asList(testViewHistory, oldHistory1, oldHistory2);

        when(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(1L)).thenReturn(allHistories);

        // Act
        userHistoryService.limitUserViewHistory(1L, 1);

        // Assert
        verify(viewHistoryRepository).deleteById(2L);
        verify(viewHistoryRepository).deleteById(3L);
    }

    @Test
    @DisplayName("限制用户浏览历史数量 - 不需要删除")
    void limitUserViewHistory_WithinLimit_ShouldNotDelete() {
        // Arrange
        List<UserViewHistory> allHistories = Collections.singletonList(testViewHistory);

        when(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(1L)).thenReturn(allHistories);

        // Act
        userHistoryService.limitUserViewHistory(1L, 5);

        // Assert
        verify(viewHistoryRepository, never()).deleteById(anyLong());
    }
}
