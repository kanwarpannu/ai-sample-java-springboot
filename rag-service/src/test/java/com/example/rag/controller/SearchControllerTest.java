package com.example.rag.controller;

import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResponse;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void search_withQuery_returns200WithResults() throws Exception {
        SearchResult result = SearchResult.builder()
                .id("doc-1")
                .title("Deployment Guide")
                .category("runbook")
                .tags(List.of("deployment", "docker"))
                .snippet("Steps for deploying the service...")
                .score(0.87)
                .build();

        SearchResponse response = SearchResponse.builder()
                .results(List.of(result))
                .totalCount(1)
                .build();

        when(ragService.search(any(SearchRequest.class))).thenReturn(response);

        SearchRequest req = new SearchRequest();
        req.setQuery("how to deploy");

        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.results[0].id").value("doc-1"))
                .andExpect(jsonPath("$.results[0].score").value(0.87));
    }

    @Test
    void search_withCategoryAndKeyword_returns200() throws Exception {
        SearchResponse response = SearchResponse.builder()
                .results(List.of())
                .totalCount(0)
                .build();

        when(ragService.search(any(SearchRequest.class))).thenReturn(response);

        SearchRequest req = new SearchRequest();
        req.setQuery("setup guide");
        req.setCategory("setup-guide");
        req.setKeyword("docker");

        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void search_blankQuery_returns400() throws Exception {
        when(ragService.search(any(SearchRequest.class))).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank"));

        SearchRequest req = new SearchRequest();
        req.setQuery("   ");

        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
