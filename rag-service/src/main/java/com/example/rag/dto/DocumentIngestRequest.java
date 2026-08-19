package com.example.rag.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DocumentIngestRequest {

    private String id;
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private String source;
    private LocalDate lastUpdated;
}
