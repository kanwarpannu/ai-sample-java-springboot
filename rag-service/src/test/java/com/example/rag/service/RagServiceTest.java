package com.example.rag.service;

import com.example.rag.domain.Document;
import com.example.rag.dto.DocumentIngestRequest;
import com.example.rag.dto.DocumentResponse;
import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResponse;
import com.example.rag.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RagService ragService;

    private Document sampleDocument(String id, String category) {
        Document doc = Document.builder()
                .id(id)
                .title("Title: " + id)
                .content("Content about " + id + " covering various topics.")
                .category(category)
                .source("https://wiki.example.com/" + id)
                .lastUpdated(LocalDate.of(2026, 1, 1))
                .build();
        doc.setTagList(List.of("tag1", "tag2"));
        return doc;
    }

    // ── ingestDocument ──────────────────────────────────────────────────────

    @Test
    void ingestDocument_withExplicitId_savesWithThatId() {
        DocumentIngestRequest req = new DocumentIngestRequest();
        req.setId("my-doc-id");
        req.setTitle("Test Doc");
        req.setContent("Some content here.");
        req.setCategory("faq");
        req.setTags(List.of("a", "b"));
        req.setLastUpdated(LocalDate.of(2026, 6, 1));

        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = ragService.ingestDocument(req);

        assertThat(response.getId()).isEqualTo("my-doc-id");
        assertThat(response.getTitle()).isEqualTo("Test Doc");
        assertThat(response.getCategory()).isEqualTo("faq");
        assertThat(response.getTags()).containsExactly("a", "b");
        verify(documentRepository).save(any(Document.class));
        verify(vectorStore).add(anyList());
    }

    @Test
    void ingestDocument_withoutId_generatesUuid() {
        DocumentIngestRequest req = new DocumentIngestRequest();
        req.setTitle("No ID Doc");
        req.setContent("Content without explicit id.");
        req.setCategory("standards");

        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = ragService.ingestDocument(req);

        assertThat(response.getId()).isNotBlank();
        assertThat(response.getId()).hasSize(36); // UUID format
    }

    // ── ingestFile ──────────────────────────────────────────────────────────

    @Test
    void ingestFile_textPlainFile_extractsContentAndDelegates() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "guide.txt", "text/plain", "Hello world content.".getBytes());

        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = ragService.ingestFile(file, "setup-guide", "internal");

        assertThat(response.getTitle()).isEqualTo("guide");
        assertThat(response.getContent()).isEqualTo("Hello world content.");
        assertThat(response.getCategory()).isEqualTo("setup-guide");
        verify(vectorStore).add(anyList());
    }

    @Test
    void ingestFile_binaryContentType_throwsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> ragService.ingestFile(file, "standards", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    // ── search ──────────────────────────────────────────────────────────────

    @Test
    void search_withResults_returnsSearchResponse() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", "doc-1");
        meta.put("title", "Deployment Guide");
        meta.put("category", "runbook");
        meta.put("tags", "deployment,docker");
        org.springframework.ai.document.Document hit =
                new org.springframework.ai.document.Document("doc-1", "Content about deployment steps.", meta);

        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(hit));

        SearchRequest req = new SearchRequest();
        req.setQuery("how to deploy");

        SearchResponse response = ragService.search(req);

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getId()).isEqualTo("doc-1");
        assertThat(response.getResults().get(0).getTitle()).isEqualTo("Deployment Guide");
        assertThat(response.getResults().get(0).getCategory()).isEqualTo("runbook");
    }

    @Test
    void search_withKeywordFilter_excludesNonMatchingResults() {
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("documentId", "doc-1");
        meta1.put("title", "Maven Guide");
        meta1.put("category", "setup-guide");
        meta1.put("tags", "maven,java");
        org.springframework.ai.document.Document hit1 =
                new org.springframework.ai.document.Document("doc-1", "All about Maven build lifecycle.", meta1);

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("documentId", "doc-2");
        meta2.put("title", "Docker Guide");
        meta2.put("category", "setup-guide");
        meta2.put("tags", "docker,containers");
        org.springframework.ai.document.Document hit2 =
                new org.springframework.ai.document.Document("doc-2", "Docker setup instructions.", meta2);

        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(hit1, hit2));

        SearchRequest req = new SearchRequest();
        req.setQuery("setup guide");
        req.setKeyword("maven");

        SearchResponse response = ragService.search(req);

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getResults().get(0).getId()).isEqualTo("doc-1");
    }

    @Test
    void search_blankQuery_throwsBadRequest() {
        SearchRequest req = new SearchRequest();
        req.setQuery("   ");

        assertThatThrownBy(() -> ragService.search(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void search_noResults_returnsEmptyResponse() {
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of());

        SearchRequest req = new SearchRequest();
        req.setQuery("something obscure");

        SearchResponse response = ragService.search(req);

        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();
    }

    // ── getDocument ─────────────────────────────────────────────────────────

    @Test
    void getDocument_found_returnsDocumentResponse() {
        Document doc = sampleDocument("arch-doc", "architecture");
        when(documentRepository.findById("arch-doc")).thenReturn(Optional.of(doc));

        DocumentResponse response = ragService.getDocument("arch-doc");

        assertThat(response.getId()).isEqualTo("arch-doc");
        assertThat(response.getCategory()).isEqualTo("architecture");
    }

    @Test
    void getDocument_notFound_throwsNotFound() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ragService.getDocument("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── listDocuments ────────────────────────────────────────────────────────

    @Test
    void listDocuments_noCategory_returnsAll() {
        when(documentRepository.findAll()).thenReturn(List.of(
                sampleDocument("doc-1", "faq"),
                sampleDocument("doc-2", "runbook")));

        List<DocumentResponse> results = ragService.listDocuments(null);

        assertThat(results).hasSize(2);
        verify(documentRepository).findAll();
    }

    @Test
    void listDocuments_withCategory_returnsByCategory() {
        when(documentRepository.findByCategory("faq")).thenReturn(List.of(
                sampleDocument("faq-1", "faq")));

        List<DocumentResponse> results = ragService.listDocuments("faq");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("faq");
        verify(documentRepository).findByCategory("faq");
    }
}
