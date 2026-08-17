package com.example.agent.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeMcpTools {

    @Tool(description = "Search the engineering knowledge base for documents matching the keyword")
    public String searchDocuments(String keyword) {
        return """
                [Knowledge Base Result for: '%s']
                Title: Engineering Onboarding Guide
                Content: Welcome to the engineering team! Here is what you need to know:
                - Repository: https://github.com/acme/engineering (clone with SSH)
                - Dev environment: Install Docker Desktop, then run `make setup`
                - Local stack: `docker compose up -d` starts all services
                - Code standards: see docs/standards.md in every repo
                - Architecture overview: Confluence → Engineering → Architecture
                - On-call rotation: PagerDuty team 'engineering-oncall'
                - Relevant contacts: platform-team@acme.com for infra questions
                """.formatted(keyword);
    }
}
