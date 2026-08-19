package com.example.rag.config;

import com.example.rag.domain.Document;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RagConfiguration implements ApplicationRunner {

    private final DocumentRepository documentRepository;
    private final RagService ragService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer vectorCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        if (vectorCount != null && vectorCount > 0) {
            log.info("[RAG] Vector store already seeded with {} documents, skipping.", vectorCount);
            return;
        }

        List<Document> documents = documentRepository.findAll();
        if (documents.isEmpty()) {
            log.info("[RAG] No documents found in database to seed vector store.");
            return;
        }

        log.info("[RAG] Seeding vector store with {} documents from database...", documents.size());
        documents.forEach(ragService::addToVectorStore);
        log.info("[RAG] Vector store seeding complete.");
    }
}
