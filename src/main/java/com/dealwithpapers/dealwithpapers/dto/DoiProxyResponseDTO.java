package com.dealwithpapers.dealwithpapers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoiProxyResponseDTO {
    private boolean success;
    private String originalDoi;
    private String redirectUrl;
    private String error;
    private String message;
} 