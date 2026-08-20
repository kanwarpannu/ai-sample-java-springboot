package com.example.knowledge.mcp.client.dto;

import java.time.LocalDate;
import java.util.List;

public record RagDocumentResponse(
        String id,
        String title,
        String content,
        String snippet,
        String category,
        List<String> tags,
        String source,
        LocalDate lastUpdated
) {}
