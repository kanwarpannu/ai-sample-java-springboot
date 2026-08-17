package com.example.agent.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeMcpToolsTest {

    private final KnowledgeMcpTools tools = new KnowledgeMcpTools();

    @Test
    void searchDocuments_returnsNonNullNonBlankResult() {
        String result = tools.searchDocuments("order service setup");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void searchDocuments_resultIncludesSearchedKeyword() {
        String keyword = "order service setup";
        String result = tools.searchDocuments(keyword);
        assertTrue(result.contains(keyword));
    }

    @Test
    void searchDocuments_differentKeywords_returnsDifferentResults() {
        String result1 = tools.searchDocuments("authentication");
        String result2 = tools.searchDocuments("deployment");
        assertNotEquals(result1, result2);
    }

    @Test
    void searchDocuments_resultContainsStructuredContent() {
        String result = tools.searchDocuments("setup");
        assertTrue(result.contains("Title:") || result.contains("Content:"));
    }
}
