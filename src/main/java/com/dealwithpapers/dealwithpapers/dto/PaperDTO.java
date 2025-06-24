package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperDTO {
    private String id;
    private String title;
    private Set<String> authors = new HashSet<>();
    private String abstractText;
    private LocalDate publishDate;
    private String conference;
    private String category;
    private String url;
} 