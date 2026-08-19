package com.example.knowledge.mcp.tool;

import com.example.knowledge.mcp.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeMcpTools {

    private final KnowledgeService knowledgeService;

    @Tool(description = "Search the knowledge base for documents matching the given keyword. Returns a list of matching documents with ID, title, category, tags, and a content snippet. Use this for broad discovery across all documentation.")
    public String searchDocuments(String keyword) {
        return knowledgeService.searchDocuments(keyword);
    }

    @Tool(description = "Retrieve the full content of a specific knowledge base document by its unique ID. Use getDocument after searchDocuments or listDocuments returns an ID you want to read in full.")
    public String getDocument(String documentId) {
        return knowledgeService.getDocument(documentId);
    }

    @Tool(description = "List all documents in the knowledge base, optionally filtered by category. Available categories: setup-guide, runbook, faq, standards, architecture. Pass null or empty string to list all documents.")
    public String listDocuments(@Nullable String category) {
        return knowledgeService.listDocuments(category);
    }

    @Tool(description = "Search for documents within a specific category using a keyword. Useful for narrowing results to a particular type of documentation such as runbooks or FAQs.")
    public String searchByCategory(String category, String keyword) {
        return knowledgeService.searchByCategory(category, keyword);
    }
}
