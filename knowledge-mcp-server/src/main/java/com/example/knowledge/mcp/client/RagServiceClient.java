package com.example.knowledge.mcp.client;

import com.example.knowledge.mcp.domain.Document;

import java.util.List;
import java.util.Optional;

public interface RagServiceClient {

    List<Document> searchDocuments(String keyword);

    Optional<Document> getDocument(String documentId);

    /**
     * Lists documents by category. Null or blank category returns all documents.
     */
    List<Document> listDocuments(String category);

    List<Document> searchByCategory(String category, String keyword);
}
