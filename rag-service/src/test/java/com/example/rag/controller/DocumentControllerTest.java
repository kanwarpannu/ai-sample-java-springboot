package com.example.rag.controller;

import com.example.rag.dto.DocumentIngestRequest;
import com.example.rag.dto.DocumentResponse;
import com.example.rag.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private DocumentResponse sampleResponse(String id) {
        return DocumentResponse.builder()
                .id(id)
                .title("Sample Doc")
                .content("Full content here.")
                .snippet("Full content here.")
                .category("faq")
                .tags(List.of("tag1", "tag2"))
                .source("https://wiki.example.com")
                .lastUpdated(LocalDate.of(2026, 6, 1))
                .build();
    }

    @Test
    void ingestJson_validRequest_returns201() throws Exception {
        DocumentIngestRequest req = new DocumentIngestRequest();
        req.setTitle("New Doc");
        req.setContent("Content of the new document.");
        req.setCategory("faq");

        when(ragService.ingestDocument(any(DocumentIngestRequest.class))).thenReturn(sampleResponse("new-doc"));

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("new-doc"))
                .andExpect(jsonPath("$.category").value("faq"));
    }

    @Test
    void ingestFile_textFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "guide.txt", "text/plain", "Content from file.".getBytes());

        when(ragService.ingestFile(any(), anyString(), any())).thenReturn(sampleResponse("file-doc"));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("category", "setup-guide"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("file-doc"));
    }

    @Test
    void listDocuments_noCategory_returns200WithList() throws Exception {
        when(ragService.listDocuments(null)).thenReturn(List.of(
                sampleResponse("doc-1"), sampleResponse("doc-2")));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listDocuments_withCategory_returns200Filtered() throws Exception {
        when(ragService.listDocuments("faq")).thenReturn(List.of(sampleResponse("faq-doc")));

        mockMvc.perform(get("/api/v1/documents").param("category", "faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("faq-doc"));
    }

    @Test
    void getDocument_notFound_returns404() throws Exception {
        when(ragService.getDocument("missing")).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: missing"));

        mockMvc.perform(get("/api/v1/documents/missing"))
                .andExpect(status().isNotFound());
    }
}
