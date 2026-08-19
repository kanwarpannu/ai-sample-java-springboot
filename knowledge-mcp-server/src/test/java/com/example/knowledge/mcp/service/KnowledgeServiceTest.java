package com.example.knowledge.mcp.service;

import com.example.knowledge.mcp.client.RagServiceClient;
import com.example.knowledge.mcp.domain.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private RagServiceClient ragServiceClient;

    @InjectMocks
    private KnowledgeService knowledgeService;

    private Document sampleDocument(String id, String category) {
        return new Document(
                id,
                "Sample Doc: " + id,
                "Detailed content about " + id + " covering setup, configuration, and troubleshooting steps.",
                category,
                List.of("sample", "test", id),
                "https://wiki.example.com/" + id,
                LocalDate.of(2026, 1, 1)
        );
    }

    @Test
    void searchDocuments_matchingKeyword_returnsFormattedList() {
        Document doc = sampleDocument("onboarding-setup", "setup-guide");
        when(ragServiceClient.searchDocuments("onboarding")).thenReturn(List.of(doc));

        String result = knowledgeService.searchDocuments("onboarding");

        assertThat(result).contains("Found 1 document(s)");
        assertThat(result).contains("onboarding-setup");
        assertThat(result).contains("setup-guide");
    }

    @Test
    void searchDocuments_noMatch_returnsNotFoundMessage() {
        when(ragServiceClient.searchDocuments("xyz")).thenReturn(List.of());

        String result = knowledgeService.searchDocuments("xyz");

        assertThat(result).isEqualTo("No documents found matching keyword: xyz");
    }

    @Test
    void getDocument_existingId_returnsFullDocumentDetails() {
        Document doc = sampleDocument("rest-api-standards", "standards");
        when(ragServiceClient.getDocument("rest-api-standards")).thenReturn(Optional.of(doc));

        String result = knowledgeService.getDocument("rest-api-standards");

        assertThat(result).contains("rest-api-standards");
        assertThat(result).contains("standards");
        assertThat(result).contains("Source:");
        assertThat(result).contains("Last Updated:");
        assertThat(result).contains("Content:");
    }

    @Test
    void getDocument_unknownId_returnsNotFoundMessage() {
        when(ragServiceClient.getDocument("unknown-id")).thenReturn(Optional.empty());

        String result = knowledgeService.getDocument("unknown-id");

        assertThat(result).isEqualTo("No document found with ID: unknown-id");
    }

    @Test
    void listDocuments_withCategory_returnsFilteredList() {
        Document doc = sampleDocument("deploy-runbook", "runbook");
        when(ragServiceClient.listDocuments("runbook")).thenReturn(List.of(doc));

        String result = knowledgeService.listDocuments("runbook");

        assertThat(result).contains("Found 1 document(s)");
        assertThat(result).contains("deploy-runbook");
        assertThat(result).contains("runbook");
    }

    @Test
    void listDocuments_nullCategory_returnsAllDocuments() {
        List<Document> all = List.of(
                sampleDocument("doc-1", "setup-guide"),
                sampleDocument("doc-2", "runbook")
        );
        when(ragServiceClient.listDocuments(null)).thenReturn(all);

        String result = knowledgeService.listDocuments(null);

        assertThat(result).contains("Found 2 document(s)");
        assertThat(result).contains("doc-1");
        assertThat(result).contains("doc-2");
    }

    @Test
    void listDocuments_unknownCategory_returnsNoDocumentsMessage() {
        when(ragServiceClient.listDocuments("unknown")).thenReturn(List.of());

        String result = knowledgeService.listDocuments("unknown");

        assertThat(result).contains("No documents found");
        assertThat(result).contains("unknown");
    }

    @Test
    void searchByCategory_matchingKeyword_returnsFilteredResults() {
        Document doc = sampleDocument("api-standards", "standards");
        when(ragServiceClient.searchByCategory("standards", "REST")).thenReturn(List.of(doc));

        String result = knowledgeService.searchByCategory("standards", "REST");

        assertThat(result).contains("Found 1 document(s)");
        assertThat(result).contains("api-standards");
    }

    @Test
    void searchByCategory_noMatch_returnsNotFoundMessage() {
        when(ragServiceClient.searchByCategory("faq", "nonexistent")).thenReturn(List.of());

        String result = knowledgeService.searchByCategory("faq", "nonexistent");

        assertThat(result).isEqualTo("No documents found in category 'faq' matching keyword: nonexistent");
    }

    @Test
    void searchDocuments_longContent_snippetIsTruncatedTo200Chars() {
        String longContent = "A".repeat(300);
        Document doc = new Document(
                "long-doc", "Long Document", longContent, "faq",
                List.of("faq"), "https://wiki.example.com/long", LocalDate.of(2026, 1, 1)
        );
        when(ragServiceClient.searchDocuments("faq")).thenReturn(List.of(doc));

        String result = knowledgeService.searchDocuments("faq");

        assertThat(result).contains("...");
        assertThat(result).doesNotContain("A".repeat(300));
    }
}
