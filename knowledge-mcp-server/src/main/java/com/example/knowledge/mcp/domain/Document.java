package com.example.knowledge.mcp.domain;

import java.time.LocalDate;
import java.util.List;

public record Document(
        String id,
        String title,
        String content,
        String category,
        List<String> tags,
        String source,
        LocalDate lastUpdated
) {}
