package com.example.knowledge.mcp.tool;

import com.example.knowledge.mcp.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeMcpTools {

    private final KnowledgeService knowledgeService;

    @Tool(description = "Search the knowledge base for documents matching the given keyword. Returns a list of matching documents with ID, title, category, tags, and a content snippet. Use this for broad discovery across all documentation.")
    public String searchDocuments(String keyword) {
        log.info("[TOOL] searchDocuments called | keyword='{}'", keyword);
        String result = knowledgeService.searchDocuments(keyword);
        log.info("[TOOL] searchDocuments completed");
        return result;
    }

    @Tool(description = "Retrieve the full content of a specific knowledge base document by its unique ID. Use getDocument after searchDocuments or listDocuments returns an ID you want to read in full.")
    public String getDocument(String documentId) {
        log.info("[TOOL] getDocument called | documentId='{}'", documentId);
        String result = knowledgeService.getDocument(documentId);
        log.info("[TOOL] getDocument completed");
        return result;
    }

    @Tool(description = "List all documents in the knowledge base, optionally filtered by category. Available categories: setup-guide, runbook, faq, standards, architecture. Pass null or empty string to list all documents.")
    public String listDocuments(@Nullable String category) {
        log.info("[TOOL] listDocuments called | category='{}'", category);
        String result = knowledgeService.listDocuments(category);
        log.info("[TOOL] listDocuments completed");
        return result;
    }

    @Tool(description = "Search for documents within a specific category using a keyword. Useful for narrowing results to a particular type of documentation such as runbooks or FAQs.")
    public String searchByCategory(String category, String keyword) {
        log.info("[TOOL] searchByCategory called | category='{}', keyword='{}'", category, keyword);
        String result = knowledgeService.searchByCategory(category, keyword);
        log.info("[TOOL] searchByCategory completed");
        return result;
    }
}
