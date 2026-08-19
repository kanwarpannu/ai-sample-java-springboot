package com.example.knowledge.mcp.tool;

import com.example.knowledge.mcp.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeMcpToolsTest {

    @Mock
    private KnowledgeService knowledgeService;

    @InjectMocks
    private KnowledgeMcpTools knowledgeMcpTools;

    @Test
    void searchDocuments_delegatesToServiceAndReturnsResult() {
        when(knowledgeService.searchDocuments("docker")).thenReturn("found 2 documents about docker");

        String result = knowledgeMcpTools.searchDocuments("docker");

        assertThat(result).isEqualTo("found 2 documents about docker");
        verify(knowledgeService).searchDocuments("docker");
    }

    @Test
    void getDocument_delegatesToServiceAndReturnsResult() {
        when(knowledgeService.getDocument("postgres-maintenance")).thenReturn("full document content");

        String result = knowledgeMcpTools.getDocument("postgres-maintenance");

        assertThat(result).isEqualTo("full document content");
        verify(knowledgeService).getDocument("postgres-maintenance");
    }

    @Test
    void listDocuments_delegatesToServiceAndReturnsResult() {
        when(knowledgeService.listDocuments("faq")).thenReturn("3 faq documents listed");

        String result = knowledgeMcpTools.listDocuments("faq");

        assertThat(result).isEqualTo("3 faq documents listed");
        verify(knowledgeService).listDocuments("faq");
    }

    @Test
    void searchByCategory_delegatesToServiceAndReturnsResult() {
        when(knowledgeService.searchByCategory("standards", "API")).thenReturn("1 standards document found");

        String result = knowledgeMcpTools.searchByCategory("standards", "API");

        assertThat(result).isEqualTo("1 standards document found");
        verify(knowledgeService).searchByCategory("standards", "API");
    }
}
