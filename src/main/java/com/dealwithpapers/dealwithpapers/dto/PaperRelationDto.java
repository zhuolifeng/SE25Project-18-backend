package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperRelationDto {
    
    private Long paperId;
    private List<RelationPaper> references;
    private List<RelationPaper> citations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationPaper {
        private String paperId;  // Semantic Scholar paper ID
        private String title;
        private List<Author> authors;
        private Integer year;
        private String doi;
        private Integer citationCount;
        private Integer influentialCitationCount;
        private String venue;
        private String abstractText;
        private List<String> intent;  // citation intent: background, methodology, result
        private OpenAccessPdf openAccessPdf;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        private String authorId;
        private String name;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAccessPdf {
        private String url;
        private String status;
    }
}