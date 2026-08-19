package com.example.knowledge.mcp.service;

import com.example.knowledge.mcp.client.RagServiceClient;
import com.example.knowledge.mcp.domain.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final RagServiceClient ragServiceClient;

    public String searchDocuments(String keyword) {
        log.info("[SERVICE] searchDocuments | keyword='{}'", keyword);
        List<Document> results = ragServiceClient.searchDocuments(keyword);
        log.info("[SERVICE] searchDocuments | found {} document(s)", results.size());
        if (results.isEmpty()) {
            return "No documents found matching keyword: " + keyword;
        }
        return formatDocumentList(results);
    }

    public String getDocument(String documentId) {
        log.info("[SERVICE] getDocument | documentId='{}'", documentId);
        Optional<Document> doc = ragServiceClient.getDocument(documentId);
        log.info("[SERVICE] getDocument | found={}", doc.isPresent());
        return doc.map(this::formatFullDocument)
                  .orElse("No document found with ID: " + documentId);
    }

    public String listDocuments(String category) {
        log.info("[SERVICE] listDocuments | category='{}'", category);
        List<Document> results = ragServiceClient.listDocuments(category);
        log.info("[SERVICE] listDocuments | found {} document(s)", results.size());
        if (results.isEmpty()) {
            String qualifier = (category != null && !category.isBlank()) ? " in category: " + category : "";
            return "No documents found" + qualifier;
        }
        return formatDocumentList(results);
    }

    public String searchByCategory(String category, String keyword) {
        log.info("[SERVICE] searchByCategory | category='{}', keyword='{}'", category, keyword);
        List<Document> results = ragServiceClient.searchByCategory(category, keyword);
        log.info("[SERVICE] searchByCategory | found {} document(s)", results.size());
        if (results.isEmpty()) {
            return "No documents found in category '" + category + "' matching keyword: " + keyword;
        }
        return formatDocumentList(results);
    }

    private String formatDocumentList(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(documents.size()).append(" document(s):\n\n");
        for (Document doc : documents) {
            sb.append("ID: ").append(doc.id()).append("\n");
            sb.append("Title: ").append(doc.title()).append("\n");
            sb.append("Category: ").append(doc.category()).append("\n");
            sb.append("Tags: ").append(String.join(", ", doc.tags())).append("\n");
            String snippet = doc.content().length() > 200
                    ? doc.content().substring(0, 200) + "..."
                    : doc.content();
            sb.append("Snippet: ").append(snippet.strip()).append("\n");
            sb.append("---\n");
        }
        return sb.toString().trim();
    }

    private String formatFullDocument(Document doc) {
        return "ID: " + doc.id() + "\n"
                + "Title: " + doc.title() + "\n"
                + "Category: " + doc.category() + "\n"
                + "Tags: " + String.join(", ", doc.tags()) + "\n"
                + "Source: " + doc.source() + "\n"
                + "Last Updated: " + doc.lastUpdated() + "\n"
                + "Content:\n" + doc.content().strip();
    }
}
