package com.example.knowledge.mcp.client.dto;

import java.util.List;

public record RagSearchResponse(List<RagSearchResult> results, int totalCount) {}
