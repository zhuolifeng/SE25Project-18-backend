package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfExtractResponseDTO {
    private boolean success;
    private String pdfUrl;
    private Map<String, Object> metadata;
    private String originalUrl;
    private String error;
    private String message;
} 