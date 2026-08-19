package com.example.rag.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {

    private List<SearchResult> results;
    private int totalCount;
}
