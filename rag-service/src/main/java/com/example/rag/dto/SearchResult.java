package com.example.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResult {

    private String id;
    private String title;
    private String category;
    private List<String> tags;
    private String snippet;
    private double score;
}
