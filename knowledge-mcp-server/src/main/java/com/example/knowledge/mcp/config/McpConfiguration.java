package com.example.knowledge.mcp.config;

import com.example.knowledge.mcp.client.RagServiceClient;
import com.example.knowledge.mcp.domain.Document;
import com.example.knowledge.mcp.tool.KnowledgeMcpTools;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class McpConfiguration {

    private final KnowledgeMcpTools knowledgeMcpTools;
    private final RagServiceClient ragServiceClient;

    @Bean
    public ToolCallbackProvider knowledgeToolCallbackProvider() {
        return MethodToolCallbackProvider.builder().toolObjects(knowledgeMcpTools).build();
    }

    @Bean
    public List<SyncResourceSpecification> knowledgeResources() {
        return ragServiceClient.listDocuments(null).stream()
                .map(this::toResourceSpecification)
                .toList();
    }

    private SyncResourceSpecification toResourceSpecification(Document doc) {
        String uri = "knowledge://" + doc.category() + "/" + doc.id();
        // Use 9-arg canonical constructor (uri,name,title,description,mimeType,size,annotations,meta,icons)
        // — the 8-arg and builder() overloads are deprecated in MCP SDK 2.0.0
        McpSchema.Resource resource = new McpSchema.Resource(
                uri, doc.id(), doc.title(), doc.title(), "text/plain", null, null, null, null
        );
        return new SyncResourceSpecification(
                resource,
                (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(uri, "text/plain", doc.content(), null)),
                        null
                )
        );
    }
}
