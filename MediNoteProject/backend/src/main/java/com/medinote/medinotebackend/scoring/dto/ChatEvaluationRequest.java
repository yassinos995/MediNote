package com.medinote.medinotebackend.scoring.dto;

public record ChatEvaluationRequest(
        String requestId,
        String prompt,
        Double relevanceScore,
        String reason,
        String category,
        Integer routingPipeline,
        String status
) {}
