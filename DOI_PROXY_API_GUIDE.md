# DOI代理和PDF提取API使用指南

## 概述

本文档介绍了后端新增的DOI代理和PDF提取服务API，用于解决前端CORS问题并提供PDF链接提取功能。

## 新增的API端点

### 1. DOI代理端点
```
GET /api/papers/proxy/doi/{doi}
```

**参数：**
- `doi`: DOI标识符，例如 `10.1109/icassp39728.2021.9413901`

**响应示例：**
```json
{
  "success": true,
  "originalDoi": "10.1109/icassp39728.2021.9413901",
  "redirectUrl": "https://ieeexplore.ieee.org/document/9413901/",
  "error": null,
  "message": "DOI代理请求成功"
}
```

### 2. PDF提取端点
```
GET /api/papers/extract-pdf?url={url}
```

**响应示例：**
```json
{
  "success": true,
  "pdfUrl": "https://arxiv.org/pdf/2101.00001.pdf",
  "metadata": {
    "arxivId": "2101.00001",
    "source": "arxiv"
  },
  "originalUrl": "https://arxiv.org/abs/2101.00001",
  "error": null,
  "message": "PDF提取成功"
}
```

### 3. DOI解析端点
```
GET /api/papers/resolve-doi/{doi}
```

**响应示例：**
```json
{
  "success": true,
  "originalDoi": "10.1109/icassp39728.2021.9413901",
  "doiProxy": {
    "success": true,
    "originalDoi": "10.1109/icassp39728.2021.9413901",
    "redirectUrl": "https://ieeexplore.ieee.org/document/9413901/",
    "error": null,
    "message": "DOI代理请求成功"
  },
  "pdfExtract": {
    "success": true,
    "pdfUrl": "https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=9413901",
    "metadata": {
      "ieeeDocumentId": "9413901",
      "source": "ieee",
      "needsProxy": true
    },
    "originalUrl": "https://ieeexplore.ieee.org/document/9413901/",
    "error": null,
    "message": "PDF提取成功"
  }
}
```

## 前端集成示例

### JavaScript 示例

```javascript
// DOI代理请求
const proxyDoi = async (doi) => {
  try {
    const response = await fetch(`/api/papers/proxy/doi/${encodeURIComponent(doi)}`);
    const data = await response.json();
    console.log('DOI代理结果:', data);
    return data;
  } catch (error) {
    console.error('DOI代理失败:', error);
    return { success: false, error: error.message };
  }
};

// 一站式DOI解析
const resolveDoi = async (doi) => {
  try {
    const response = await fetch(`/api/papers/resolve-doi/${encodeURIComponent(doi)}`);
    const data = await response.json();
    console.log('DOI解析结果:', data);
    return data;
  } catch (error) {
    console.error('DOI解析失败:', error);
    return { success: false, error: error.message };
  }
};

// 使用示例
const handleDoiSearch = async (doi) => {
  const result = await resolveDoi(doi);
  if (result.success) {
    console.log('重定向URL:', result.doiProxy.redirectUrl);
    if (result.pdfExtract && result.pdfExtract.success) {
      console.log('PDF链接:', result.pdfExtract.pdfUrl);
    }
  }
};
```

## 支持的URL类型

1. **DOI URLs**: `https://doi.org/10.1109/icassp39728.2021.9413901`
2. **ArXiv URLs**: `https://arxiv.org/abs/2101.00001`
3. **IEEE URLs**: `https://ieeexplore.ieee.org/document/9413901/`
4. **ACM URLs**: `https://dl.acm.org/doi/10.1145/3394486.3403176`
5. **Springer URLs**: `https://link.springer.com/article/10.1007/s00371-021-02056-0`

## 注意事项

1. **CORS配置**：已配置允许 `http://localhost:3000` 跨域访问
2. **超时设置**：HTTP请求超时时间设置为30秒
3. **错误处理**：所有API都包含详细的错误信息
4. **日志记录**：所有请求都会记录详细日志用于调试 