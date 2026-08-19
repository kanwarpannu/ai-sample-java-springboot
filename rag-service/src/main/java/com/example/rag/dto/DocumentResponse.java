package com.example.rag.dto;

import com.example.rag.domain.Document;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DocumentResponse {

    private String id;
    private String title;
    private String content;
    private String snippet;
    private String category;
    private List<String> tags;
    private String source;
    private LocalDate lastUpdated;

    public static DocumentResponse from(Document doc) {
        String content = doc.getContent();
        String snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .content(content)
                .snippet(snippet)
                .category(doc.getCategory())
                .tags(doc.getTagList())
                .source(doc.getSource())
                .lastUpdated(doc.getLastUpdated())
                .build();
    }
}
