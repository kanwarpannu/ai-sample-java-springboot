package com.example.knowledge.mcp.client;

import com.example.knowledge.mcp.domain.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpRagServiceClientTest {

    private MockRestServiceServer mockServer;
    private HttpRagServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8083");
        mockServer = MockRestServiceServer.createServer(builder);
        client = new HttpRagServiceClient(builder.build());
    }

    @Test
    void searchDocuments_returnsDocumentsMappedFromSnippet() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"query":"docker"}
                        """))
                .andRespond(withSuccess("""
                        {"results":[{"id":"d1","title":"Docker Guide","category":"runbook",
                         "tags":["docker","deploy"],"snippet":"Deploy with docker compose","score":0.9}],
                         "totalCount":1}
                        """, MediaType.APPLICATION_JSON));

        List<Document> results = client.searchDocuments("docker");

        assertThat(results).hasSize(1);
        Document doc = results.get(0);
        assertThat(doc.id()).isEqualTo("d1");
        assertThat(doc.title()).isEqualTo("Docker Guide");
        assertThat(doc.content()).isEqualTo("Deploy with docker compose");
        assertThat(doc.category()).isEqualTo("runbook");
        assertThat(doc.tags()).containsExactly("docker", "deploy");
        assertThat(doc.source()).isNull();
        assertThat(doc.lastUpdated()).isNull();
        mockServer.verify();
    }

    @Test
    void searchDocuments_emptyResults_returnsEmptyList() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"results":[],"totalCount":0}
                        """, MediaType.APPLICATION_JSON));

        List<Document> results = client.searchDocuments("xyz");

        assertThat(results).isEmpty();
        mockServer.verify();
    }

    @Test
    void getDocument_existingId_returnsFullDocument() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/documents/doc-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"doc-1","title":"Platform Setup","content":"Full content here.",
                         "snippet":"Full content here.","category":"setup-guide",
                         "tags":["setup"],"source":"https://wiki.example.com/setup",
                         "lastUpdated":"2026-08-01"}
                        """, MediaType.APPLICATION_JSON));

        Optional<Document> result = client.getDocument("doc-1");

        assertThat(result).isPresent();
        Document doc = result.get();
        assertThat(doc.id()).isEqualTo("doc-1");
        assertThat(doc.content()).isEqualTo("Full content here.");
        assertThat(doc.source()).isEqualTo("https://wiki.example.com/setup");
        assertThat(doc.lastUpdated()).isEqualTo(LocalDate.of(2026, 8, 1));
        mockServer.verify();
    }

    @Test
    void getDocument_notFound_returnsOptionalEmpty() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/documents/missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Optional<Document> result = client.getDocument("missing");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void listDocuments_nullCategory_noQueryParam() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/documents"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id":"d1","title":"Doc 1","content":"Content 1","snippet":"Content 1",
                           "category":"faq","tags":[],"source":null,"lastUpdated":"2026-07-01"},
                          {"id":"d2","title":"Doc 2","content":"Content 2","snippet":"Content 2",
                           "category":"runbook","tags":["k8s"],"source":"https://wiki.example.com","lastUpdated":"2026-07-15"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<Document> results = client.listDocuments(null);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo("d1");
        assertThat(results.get(1).id()).isEqualTo("d2");
        assertThat(results.get(1).source()).isEqualTo("https://wiki.example.com");
        mockServer.verify();
    }

    @Test
    void listDocuments_withCategory_appendsQueryParam() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/documents?category=runbook"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"r1","title":"Deploy Runbook","content":"Steps to deploy.",
                          "snippet":"Steps to deploy.","category":"runbook",
                          "tags":["deploy"],"source":null,"lastUpdated":"2026-06-01"}]
                        """, MediaType.APPLICATION_JSON));

        List<Document> results = client.listDocuments("runbook");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo("r1");
        assertThat(results.get(0).category()).isEqualTo("runbook");
        mockServer.verify();
    }

    @Test
    void searchByCategory_includesCategoryInBody() {
        mockServer.expect(requestTo("http://localhost:8083/api/v1/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"query":"docker","category":"runbook"}
                        """))
                .andRespond(withSuccess("""
                        {"results":[{"id":"r2","title":"Docker Runbook","category":"runbook",
                         "tags":["docker"],"snippet":"Docker deployment steps","score":0.85}],
                         "totalCount":1}
                        """, MediaType.APPLICATION_JSON));

        List<Document> results = client.searchByCategory("runbook", "docker");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).category()).isEqualTo("runbook");
        assertThat(results.get(0).content()).isEqualTo("Docker deployment steps");
        mockServer.verify();
    }
}
