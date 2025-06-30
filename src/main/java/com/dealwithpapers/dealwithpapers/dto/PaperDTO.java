package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperDTO {
    private Long id;
    private String doi;
    private String title;
    private Set<String> authors = new HashSet<>();
    private String abstractText;
    private Integer year;
    private String journal;
    private String category;
    private String url;
} 