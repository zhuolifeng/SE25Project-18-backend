package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.DoiProxyResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.PdfExtractResponseDTO;

public interface DoiProxyService {
    
    /**
     * 代理DOI请求，获取重定向后的URL
     * @param doi DOI标识符
     * @return 代理响应结果
     */
    DoiProxyResponseDTO proxyDoiRequest(String doi);
    
    /**
     * 从URL提取PDF链接
     * @param url 原始URL
     * @return PDF提取响应结果
     */
    PdfExtractResponseDTO extractPdfFromUrl(String url);
} 