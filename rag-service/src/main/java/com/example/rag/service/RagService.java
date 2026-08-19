package com.example.rag.service;

import com.example.rag.domain.Document;
import com.example.rag.dto.DocumentIngestRequest;
import com.example.rag.dto.DocumentResponse;
import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResponse;
import com.example.rag.dto.SearchResult;
import com.example.rag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    @Transactional
    public DocumentResponse ingestDocument(DocumentIngestRequest request) {
        String id = (request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : UUID.randomUUID().toString();

        Document doc = Document.builder()
                .id(id)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .source(request.getSource())
                .lastUpdated(request.getLastUpdated() != null ? request.getLastUpdated() : LocalDate.now())
                .build();
        doc.setTagList(request.getTags());

        documentRepository.save(doc);
        addToVectorStore(doc);

        log.info("[RAG] ingested document id='{}' title='{}'", id, doc.getTitle());
        return DocumentResponse.from(doc);
    }

    @Transactional
    public DocumentResponse ingestFile(MultipartFile file, String category, String source) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("text/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only plain text files are supported. Received: " + contentType);
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
        String title = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;

        DocumentIngestRequest req = new DocumentIngestRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setCategory(category);
        req.setSource(source != null ? source : filename);
        req.setLastUpdated(LocalDate.now());

        return ingestDocument(req);
    }

    public SearchResponse search(SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank");
        }

        Builder searchBuilder = org.springframework.ai.vectorstore.SearchRequest.builder()
                .query(request.getQuery())
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD);

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            searchBuilder.filterExpression("category == '" + request.getCategory() + "'");
        }

        List<org.springframework.ai.document.Document> hits = vectorStore.similaritySearch(searchBuilder.build());
        log.info("[RAG] search query='{}' category='{}' raw hits={}", request.getQuery(), request.getCategory(), hits.size());

        String keyword = request.getKeyword();
        List<SearchResult> results = hits.stream()
                .filter(hit -> matchesKeyword(hit, keyword))
                .map(hit -> toSearchResult(hit))
                .toList();

        log.info("[RAG] search after keyword filter={}", results.size());
        return SearchResponse.builder()
                .results(results)
                .totalCount(results.size())
                .build();
    }

    public DocumentResponse getDocument(String id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + id));
        return DocumentResponse.from(doc);
    }

    public List<DocumentResponse> listDocuments(String category) {
        List<Document> docs = (category != null && !category.isBlank())
                ? documentRepository.findByCategory(category)
                : documentRepository.findAll();
        return docs.stream().map(DocumentResponse::from).toList();
    }

    public void addToVectorStore(Document doc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", doc.getId());
        metadata.put("title", doc.getTitle());
        metadata.put("category", doc.getCategory() != null ? doc.getCategory() : "");
        metadata.put("tags", doc.getTags() != null ? doc.getTags() : "");
        metadata.put("source", doc.getSource() != null ? doc.getSource() : "");

        String vectorId = UUID.nameUUIDFromBytes(doc.getId().getBytes(StandardCharsets.UTF_8)).toString();
        org.springframework.ai.document.Document aiDoc =
                new org.springframework.ai.document.Document(vectorId, doc.getContent(), metadata);
        vectorStore.add(List.of(aiDoc));
    }

    private boolean matchesKeyword(org.springframework.ai.document.Document hit, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase();
        String text = hit.getText() != null ? hit.getText().toLowerCase() : "";
        Map<String, Object> meta = hit.getMetadata();
        String title = meta.getOrDefault("title", "").toString().toLowerCase();
        String tags = meta.getOrDefault("tags", "").toString().toLowerCase();
        return text.contains(lower) || title.contains(lower) || tags.contains(lower);
    }

    private SearchResult toSearchResult(org.springframework.ai.document.Document hit) {
        Map<String, Object> meta = hit.getMetadata();
        String docId = meta.getOrDefault("documentId", hit.getId()).toString();
        String title = meta.getOrDefault("title", "").toString();
        String category = meta.getOrDefault("category", "").toString();
        String tagsRaw = meta.getOrDefault("tags", "").toString();
        List<String> tags = tagsRaw.isBlank() ? List.of()
                : List.of(tagsRaw.split(",")).stream().map(String::trim).filter(t -> !t.isEmpty()).toList();

        String text = hit.getText() != null ? hit.getText() : "";
        String snippet = text.length() > 200 ? text.substring(0, 200) + "..." : text;

        Double score = hit.getScore();
        return SearchResult.builder()
                .id(docId)
                .title(title)
                .category(category)
                .tags(tags)
                .snippet(snippet)
                .score(score != null ? score : 0.0)
                .build();
    }
}
