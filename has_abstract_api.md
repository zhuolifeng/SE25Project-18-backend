# 论文摘要状态(hasAbstract)字段使用指南

## API 概述

以下API可用于访问和操作论文的`hasAbstract`字段：

### 1. 获取论文信息时

以下API会返回包含`hasAbstract`字段的论文信息：

```
GET /api/papers/{id}
```

返回示例：
```json
{
  "id": 1,
  "doi": "10.1234/example",
  "title": "论文标题",
  "authors": ["作者1", "作者2"],
  "abstractText": "论文摘要内容...",
  "hasAbstract": true,
  "year": 2023,
  "journal": "期刊名称",
  "category": "分类",
  "url": "https://example.com/paper"
}
```

### 2. 搜索论文时

获取所有论文：
```
GET /api/papers
```

使用搜索API：
```
POST /api/papers/search
```

请求体示例：
```json
{
  "searchTerm": "搜索关键词",
  "year": 2023
}
```

### 3. 创建或更新论文时

创建新论文：
```
POST /api/papers
```

更新论文：
```
PUT /api/papers/{id}
```

请求体示例：
```json
{
  "title": "论文标题",
  "authors": ["作者1", "作者2"],
  "abstractText": "论文摘要内容...",
  "hasAbstract": true,
  "year": 2023,
  "journal": "期刊名称",
  "category": "分类",
  "url": "https://example.com/paper"
}
```

## 注意事项

1. `hasAbstract`字段需要由前端明确设置，系统不会根据`abstractText`的内容自动设置
2. 如果未指定，该字段默认值为`false`
3. 即使存在摘要文本，若要将论文标记为有摘要，必须明确设置`hasAbstract`为`true`

## 前端应用场景

前端可以利用这个字段来：

1. 在论文列表中显示是否有摘要的标记
2. 在搜索过滤中添加"只显示有摘要的论文"选项
3. 在论文详情页面根据是否有摘要来调整UI显示
4. 在批量导入论文时标记哪些论文需要额外获取摘要 