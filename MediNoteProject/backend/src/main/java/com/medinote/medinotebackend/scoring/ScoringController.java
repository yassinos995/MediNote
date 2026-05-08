package com.medinote.medinotebackend.scoring;

import com.medinote.medinotebackend.scoring.dto.ChatEvaluationRequest;
import com.medinote.medinotebackend.scoring.dto.ScoreCardResponse;
import com.medinote.medinotebackend.scoring.dto.ScoringEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scoring")
@CrossOrigin("*")
public class ScoringController {

    private final ScoringService service;

    public ScoringController(ScoringService service) {
        this.service = service;
    }

    @PostMapping("/events")
    public ResponseEntity<ScoreCardResponse> recordEvent(
            @RequestBody ScoringEventRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.recordEvent(authentication.getName(), request));
    }

    @PostMapping("/chat-evaluation")
    public ResponseEntity<ScoreCardResponse> recordChatEvaluation(
            @RequestBody ChatEvaluationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.recordChatEvaluation(authentication.getName(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<ScoreCardResponse> myScore(Authentication authentication) {
        return ResponseEntity.ok(service.scoreFor(authentication.getName()));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<ScoreCardResponse>> adminScores() {
        return ResponseEntity.ok(service.adminScores());
    }
}
