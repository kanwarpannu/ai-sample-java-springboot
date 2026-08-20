package com.example.knowledge.mcp.client.dto;

import java.util.List;

public record RagSearchResult(
        String id,
        String title,
        String category,
        List<String> tags,
        String snippet,
        double score
) {}
