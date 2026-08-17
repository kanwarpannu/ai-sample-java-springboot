package com.example.agent.dto;

import reactor.core.publisher.Flux;

public record StreamResult(String sessionId, Flux<String> tokenFlux) {}
