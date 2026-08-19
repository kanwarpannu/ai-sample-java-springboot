package com.example.rag.repository;

import com.example.rag.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByCategory(String category);
}
