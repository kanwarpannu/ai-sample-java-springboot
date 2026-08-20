package com.example.knowledge.mcp.client;

import com.example.knowledge.mcp.client.dto.RagDocumentResponse;
import com.example.knowledge.mcp.client.dto.RagSearchRequest;
import com.example.knowledge.mcp.client.dto.RagSearchResponse;
import com.example.knowledge.mcp.client.dto.RagSearchResult;
import com.example.knowledge.mcp.domain.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class HttpRagServiceClient implements RagServiceClient {

    private final RestClient restClient;

    public HttpRagServiceClient(RestClient ragServiceRestClient) {
        this.restClient = ragServiceRestClient;
    }

    @Override
    public List<Document> searchDocuments(String keyword) {
        log.info("[CLIENT] searchDocuments | keyword='{}'", keyword);
        RagSearchRequest request = new RagSearchRequest(keyword, null, null);
        RagSearchResponse response = restClient.post()
                .uri("/api/v1/search")
                .body(request)
                .retrieve()
                .body(RagSearchResponse.class);
        List<Document> results = response.results().stream()
                .map(this::fromSearchResult)
                .toList();
        log.info("[CLIENT] searchDocuments | returning {} document(s)", results.size());
        return results;
    }

    @Override
    public Optional<Document> getDocument(String documentId) {
        log.info("[CLIENT] getDocument | documentId='{}'", documentId);
        try {
            RagDocumentResponse response = restClient.get()
                    .uri("/api/v1/documents/{id}", documentId)
                    .retrieve()
                    .body(RagDocumentResponse.class);
            log.info("[CLIENT] getDocument | found=true");
            return Optional.of(fromDocumentResponse(response));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.info("[CLIENT] getDocument | found=false");
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public List<Document> listDocuments(String category) {
        log.info("[CLIENT] listDocuments | category='{}'", category);
        List<RagDocumentResponse> responses = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/api/v1/documents");
                    if (category != null && !category.isBlank()) {
                        b.queryParam("category", category);
                    }
                    return b.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<RagDocumentResponse>>() {});
        List<Document> results = responses.stream()
                .map(this::fromDocumentResponse)
                .toList();
        log.info("[CLIENT] listDocuments | returning {} document(s)", results.size());
        return results;
    }

    @Override
    public List<Document> searchByCategory(String category, String keyword) {
        log.info("[CLIENT] searchByCategory | category='{}', keyword='{}'", category, keyword);
        RagSearchRequest request = new RagSearchRequest(keyword, category, null);
        RagSearchResponse response = restClient.post()
                .uri("/api/v1/search")
                .body(request)
                .retrieve()
                .body(RagSearchResponse.class);
        List<Document> results = response.results().stream()
                .map(this::fromSearchResult)
                .toList();
        log.info("[CLIENT] searchByCategory | returning {} document(s)", results.size());
        return results;
    }

    private Document fromSearchResult(RagSearchResult r) {
        return new Document(
                r.id(),
                r.title(),
                r.snippet(),
                r.category(),
                r.tags() != null ? r.tags() : List.of(),
                null,
                null
        );
    }

    private Document fromDocumentResponse(RagDocumentResponse r) {
        return new Document(
                r.id(),
                r.title(),
                r.content(),
                r.category(),
                r.tags() != null ? r.tags() : List.of(),
                r.source(),
                r.lastUpdated()
        );
    }
}
