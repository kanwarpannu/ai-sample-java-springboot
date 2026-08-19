package com.example.rag.dto;

import lombok.Data;

@Data
public class SearchRequest {

    private String query;
    private String category;
    private String keyword;
}
