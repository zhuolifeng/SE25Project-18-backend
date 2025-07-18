package com.dealwithpapers.dealwithpapers;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperRelationDto;
import com.dealwithpapers.dealwithpapers.entity.PaperRelation;
import com.dealwithpapers.dealwithpapers.service.CitationDataService;
import com.dealwithpapers.dealwithpapers.service.PaperRelationService;
import com.dealwithpapers.dealwithpapers.service.PaperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 引用数据缓存功能测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class CitationCacheTest {
    
    @Autowired
    private CitationDataService citationDataService;
    
    @Autowired
    private PaperRelationService paperRelationService;
    
    @Autowired
    private PaperService paperService;
    
    @Test
    public void testCitationDataServiceBasicFunction() {
        System.out.println("=== 测试CitationDataService基本功能 ===");
        
        try {
            // 测试通过标题获取引用数据
            String testTitle = "Attention Is All You Need";
            PaperRelationDto relationDto = citationDataService.getCitationDataByTitle(testTitle);
            
            if (relationDto != null) {
                int refCount = relationDto.getReferences() != null ? relationDto.getReferences().size() : 0;
                int citCount = relationDto.getCitations() != null ? relationDto.getCitations().size() : 0;
                
                System.out.println("✅ 成功获取引用数据:");
                System.out.println("   - 引用论文数: " + refCount);
                System.out.println("   - 被引论文数: " + citCount);
                
                // 打印前几个引用论文的信息
                if (relationDto.getReferences() != null && !relationDto.getReferences().isEmpty()) {
                    System.out.println("   - 前3个引用论文:");
                    for (int i = 0; i < Math.min(3, relationDto.getReferences().size()); i++) {
                        PaperRelationDto.RelationPaper paper = relationDto.getReferences().get(i);
                        System.out.println("     " + (i+1) + ". " + paper.getTitle());
                    }
                }
            } else {
                System.out.println("⚠️ 未获取到引用数据");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testPaperSearchWithCitations() {
        System.out.println("=== 测试PaperService搜索并获取引用数据 ===");
        
        try {
            // 测试搜索论文并获取引用数据
            String searchTerm = "ResNet";
            List<PaperDTO> papers = paperService.searchByTermWithCitations(searchTerm, true);
            
            System.out.println("🔍 搜索结果:");
            System.out.println("   - 找到论文数: " + papers.size());
            
            if (!papers.isEmpty()) {
                PaperDTO firstPaper = papers.get(0);
                System.out.println("   - 第一篇论文: " + firstPaper.getTitle());
                System.out.println("   - 论文ID: " + firstPaper.getId());
                System.out.println("   - DOI: " + firstPaper.getDoi());
                
                // 检查是否有引用数据保存到数据库
                if (firstPaper.getId() != null) {
                    List<PaperRelation> references = paperRelationService.getPaperReferences(firstPaper.getId());
                    List<PaperRelation> citations = paperRelationService.getPaperCitations(firstPaper.getId());
                    
                    System.out.println("   - 数据库中的引用数据:");
                    System.out.println("     引用论文数: " + references.size());
                    System.out.println("     被引论文数: " + citations.size());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testPaperRelationServiceSaveAndRetrieve() {
        System.out.println("=== 测试PaperRelationService保存和检索功能 ===");
        
        try {
            // 创建测试数据
            PaperRelationDto testDto = createTestRelationDto();
            
            if (testDto != null) {
                // 保存引用关系
                paperRelationService.savePaperRelations(testDto);
                System.out.println("✅ 成功保存引用关系");
                
                // 检索引用关系
                List<PaperRelation> references = paperRelationService.getPaperReferences(testDto.getPaperId());
                List<PaperRelation> citations = paperRelationService.getPaperCitations(testDto.getPaperId());
                
                System.out.println("📊 检索结果:");
                System.out.println("   - 引用论文数: " + references.size());
                System.out.println("   - 被引论文数: " + citations.size());
                
                // 显示优先级分数
                if (!references.isEmpty()) {
                    System.out.println("   - 引用论文优先级分数:");
                    for (int i = 0; i < Math.min(3, references.size()); i++) {
                        PaperRelation rel = references.get(i);
                        System.out.println("     " + rel.getTargetTitle() + " -> " + rel.getPriorityScore());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试用的引用关系数据
     */
    private PaperRelationDto createTestRelationDto() {
        try {
            // 假设我们有一个测试论文ID
            List<PaperDTO> papers = paperService.searchByTerm("machine learning");
            if (papers.isEmpty()) {
                System.out.println("⚠️ 未找到测试论文，跳过保存测试");
                return null;
            }
            
            PaperDTO testPaper = papers.get(0);
            System.out.println("📄 使用测试论文: " + testPaper.getTitle());
            
            // 通过CitationDataService获取真实的引用数据
            PaperRelationDto relationDto = null;
            
            if (testPaper.getDoi() != null && !testPaper.getDoi().trim().isEmpty()) {
                relationDto = citationDataService.getCitationDataByDoi(testPaper.getDoi());
            }
            
            if (relationDto == null && testPaper.getTitle() != null) {
                relationDto = citationDataService.getCitationDataByTitle(testPaper.getTitle());
            }
            
            if (relationDto != null) {
                relationDto.setPaperId(testPaper.getId());
            }
            
            return relationDto;
            
        } catch (Exception e) {
            System.err.println("创建测试数据失败: " + e.getMessage());
            return null;
        }
    }
    
    @Test
    public void testFullCitationCacheWorkflow() {
        System.out.println("=== 测试完整的引用缓存工作流程 ===");
        
        try {
            // 1. 搜索论文
            String searchTerm = "transformer";
            List<PaperDTO> papers = paperService.searchByTermWithCitations(searchTerm, false); // 先不获取引用数据
            
            if (papers.isEmpty()) {
                System.out.println("⚠️ 未找到测试论文");
                return;
            }
            
            PaperDTO testPaper = papers.get(0);
            System.out.println("1. 📄 找到测试论文: " + testPaper.getTitle());
            
            // 2. 检查数据库中是否已有引用数据
            List<PaperRelation> existingRefs = paperRelationService.getPaperReferences(testPaper.getId());
            List<PaperRelation> existingCits = paperRelationService.getPaperCitations(testPaper.getId());
            
            System.out.println("2. 📊 现有缓存数据: " + existingRefs.size() + " 引用, " + existingCits.size() + " 被引");
            
            // 3. 如果没有缓存数据，从API获取并保存
            if (existingRefs.isEmpty() && existingCits.isEmpty()) {
                System.out.println("3. 🌐 从API获取引用数据...");
                paperService.searchByTermWithCitations(testPaper.getTitle(), true); // 获取引用数据
                
                // 重新检查数据库
                List<PaperRelation> newRefs = paperRelationService.getPaperReferences(testPaper.getId());
                List<PaperRelation> newCits = paperRelationService.getPaperCitations(testPaper.getId());
                
                System.out.println("4. ✅ 新缓存数据: " + newRefs.size() + " 引用, " + newCits.size() + " 被引");
            } else {
                System.out.println("3. ✅ 使用现有缓存数据");
            }
            
            System.out.println("🎯 引用缓存工作流程测试完成");
            
        } catch (Exception e) {
            System.err.println("❌ 工作流程测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}