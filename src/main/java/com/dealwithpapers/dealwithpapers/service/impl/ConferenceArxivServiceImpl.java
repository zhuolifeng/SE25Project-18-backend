package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.service.ConferenceArxivService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

@Service
public class ConferenceArxivServiceImpl implements ConferenceArxivService {
    @Override
    public List<PaperDTO> searchConferencePapers(PaperSearchDTO searchDTO) {
        List<PaperDTO> result = new ArrayList<>();
        try {
            String baseUrl = "https://export.arxiv.org/api/query";
            StringBuilder query = new StringBuilder();
            // 会议名作为cat:或关键词
            if (searchDTO.getConferenceName() != null && !searchDTO.getConferenceName().isEmpty()) {
                // 优先cat:，否则all:
                query.append("all:").append(searchDTO.getConferenceName());
            }
            // 年份、主题、关键词
            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isEmpty()) {
                if (query.length() > 0) query.append("+AND+");
                query.append("all:").append(searchDTO.getKeyword());
            }
            if (searchDTO.getTopic() != null && !searchDTO.getTopic().isEmpty()) {
                if (query.length() > 0) query.append("+AND+");
                query.append("all:").append(searchDTO.getTopic());
            }
            // 年份筛选，拼接到arXiv查询
            if (searchDTO.getYear() != null) {
                if (query.length() > 0) query.append("+AND+");
                query.append("submittedDate:[").append(searchDTO.getYear()).append("01010000 TO ").append(searchDTO.getYear()).append("12312359]");
            }
            if (query.length() == 0) {
                query.append("all:*");
            }
            // 日志输出
            System.out.println("[Conference] conferenceName: " + searchDTO.getConferenceName());
            System.out.println("[Conference] year: " + searchDTO.getYear());
            System.out.println("[Conference] keyword: " + searchDTO.getKeyword());
            System.out.println("[Conference] topic: " + searchDTO.getTopic());
            System.out.println("[Conference] 最终拼接的query: " + query.toString());
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("search_query", query.toString())
                    .queryParam("start", 0)
                    .queryParam("max_results", 10)
                    .queryParam("sortBy", "submittedDate")
                    .queryParam("sortOrder", "descending")
                    .build().toUri();
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                String xml = response.getBody();
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
                NodeList entries = doc.getElementsByTagName("entry");
                for (int i = 0; i < entries.getLength(); i++) {
                    Element entry = (Element) entries.item(i);
                    PaperDTO paper = new PaperDTO();
                    paper.setTitle(getTagText(entry, "title"));
                    HashSet<String> authorsSet = new HashSet<>();
                    for (String name : getAuthors(entry).split(", ")) {
                        if (!name.isBlank()) authorsSet.add(name.trim());
                    }
                    paper.setAuthors(authorsSet);
                    paper.setAbstractText(getTagText(entry, "summary"));
                    paper.setYear(getPublishedYear(entry));
                    paper.setJournal("arXiv");
                    paper.setUrl(getTagText(entry, "id"));
                    result.add(paper);
                }
            }
        } catch (Exception e) {
            // 可记录日志
            System.out.println("[Conference] 查询异常: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<PaperDTO> searchArxivPapers(PaperSearchDTO searchDTO) {
        List<PaperDTO> result = new ArrayList<>();
        try {
            String baseUrl = "https://export.arxiv.org/api/query";
            StringBuilder query = new StringBuilder();
            // 分类
            if (searchDTO.getArxivCategory() != null && !searchDTO.getArxivCategory().isEmpty()) {
                query.append("cat:").append(searchDTO.getArxivCategory());
            }
            // 关键词
            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isEmpty()) {
                if (query.length() > 0) query.append("+AND+");
                query.append("all:").append(searchDTO.getKeyword());
            }
            if (query.length() == 0) {
                query.append("all:*"); // 默认返回全部
            }
            // 日志输出
            System.out.println("[Arxiv] arxivCategory: " + searchDTO.getArxivCategory());
            System.out.println("[Arxiv] keyword: " + searchDTO.getKeyword());
            System.out.println("[Arxiv] 最终拼接的query: " + query.toString());
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("search_query", query.toString())
                    .queryParam("start", 0)
                    .queryParam("max_results", 10)
                    .queryParam("sortBy", "submittedDate")
                    .queryParam("sortOrder", "descending")
                    .build().toUri();
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                String xml = response.getBody();
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
                NodeList entries = doc.getElementsByTagName("entry");
                for (int i = 0; i < entries.getLength(); i++) {
                    Element entry = (Element) entries.item(i);
                    PaperDTO paper = new PaperDTO();
                    paper.setTitle(getTagText(entry, "title"));
                    // 修正：authors为Set<String>
                    HashSet<String> authorsSet = new HashSet<>();
                    for (String name : getAuthors(entry).split(", ")) {
                        if (!name.isBlank()) authorsSet.add(name.trim());
                    }
                    paper.setAuthors(authorsSet);
                    paper.setAbstractText(getTagText(entry, "summary"));
                    paper.setYear(getPublishedYear(entry));
                    paper.setJournal("arXiv");
                    paper.setUrl(getTagText(entry, "id"));
                    result.add(paper);
                }
            }
        } catch (Exception e) {
            // 可记录日志
            System.out.println("[Arxiv] 查询异常: " + e.getMessage());
        }
        return result;
    }

    // 辅助方法：获取标签文本
    private String getTagText(Element entry, String tag) {
        NodeList list = entry.getElementsByTagName(tag);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }
    // 辅助方法：获取作者
    private String getAuthors(Element entry) {
        NodeList authors = entry.getElementsByTagName("author");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < authors.getLength(); i++) {
            NodeList nameList = ((Element) authors.item(i)).getElementsByTagName("name");
            if (nameList.getLength() > 0) {
                names.add(nameList.item(0).getTextContent().trim());
            }
        }
        return String.join(", ", names);
    }
    // 辅助方法：获取年份
    private Integer getPublishedYear(Element entry) {
        NodeList published = entry.getElementsByTagName("published");
        if (published.getLength() > 0) {
            String date = published.item(0).getTextContent();
            if (date != null && date.length() >= 4) {
                try {
                    return Integer.parseInt(date.substring(0, 4));
                } catch (Exception e) { }
            }
        }
        return null;
    }
} 