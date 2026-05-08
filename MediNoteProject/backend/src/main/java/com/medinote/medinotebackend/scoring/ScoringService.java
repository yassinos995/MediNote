package com.medinote.medinotebackend.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medinote.medinotebackend.scoring.dto.ChatEvaluationRequest;
import com.medinote.medinotebackend.scoring.dto.ScoreCardResponse;
import com.medinote.medinotebackend.scoring.dto.ScoringEventRequest;
import com.medinote.medinotebackend.user.Role;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ScoringService {

    private static final List<String> ADMIN_FEATURES = List.of("dashboard", "users", "report_analysis", "profile", "chat");
    private static final List<String> STAFF_FEATURES = List.of("dashboard", "report_analysis", "calendar", "profile", "chat");
    private static final List<String> DELEGATE_FEATURES = List.of("dashboard", "calendar", "report_analysis", "profile", "chat");

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final ObjectMapper mapper;

    public ScoringService(JdbcTemplate jdbc, UserRepository users, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public ScoreCardResponse recordEvent(String email, ScoringEventRequest request) {
        User user = user(email);
        String eventType = clean(request.eventType(), "feature_visit", 64);
        String feature = clean(request.feature(), null, 80);
        String surface = clean(request.surface(), "unknown", 40);

        jdbc.update(
                """
                INSERT INTO user_usage_events
                    (user_id, email, event_type, feature, surface, metadata_json)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                user.getId(),
                user.getEmail(),
                eventType,
                feature,
                surface,
                metadataJson(request)
        );

        return recalculate(user);
    }

    @Transactional
    public ScoreCardResponse recordChatEvaluation(String email, ChatEvaluationRequest request) {
        User user = user(email);
        double relevance = clamp(round1(request.relevanceScore() == null ? 0.0 : request.relevanceScore()));

        jdbc.update(
                """
                INSERT INTO user_chat_evaluations
                    (user_id, email, request_id, prompt, relevance_score, reason, category, routing_pipeline, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(),
                user.getEmail(),
                clean(request.requestId(), null, 80),
                Objects.requireNonNullElse(request.prompt(), ""),
                relevance,
                clean(request.reason(), null, 500),
                clean(request.category(), null, 80),
                request.routingPipeline(),
                clean(request.status(), null, 32)
        );

        jdbc.update(
                """
                INSERT INTO user_usage_events
                    (user_id, email, event_type, feature, surface, metadata_json)
                VALUES (?, ?, 'feature_visit', 'chat', 'agent_api', ?)
                """,
                user.getId(),
                user.getEmail(),
                "{\"source\":\"chat_evaluation\"}"
        );

        return recalculate(user);
    }

    @Transactional
    public ScoreCardResponse scoreFor(String email) {
        return recalculate(user(email));
    }

    @Transactional
    public List<ScoreCardResponse> adminScores() {
        return users.findAll().stream()
                .map(this::recalculate)
                .sorted(Comparator.comparingDouble(ScoreCardResponse::overallScore).reversed())
                .toList();
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    private ScoreCardResponse recalculate(User user) {
        double relevanceScore = avgRelevance(user.getId());
        int chatQuestions = intValue("""
                SELECT COUNT(*) FROM user_chat_evaluations WHERE user_id = ?
                """, user.getId());
        int totalSessions = intValue("""
                SELECT COUNT(*) FROM user_usage_events
                WHERE user_id = ? AND event_type = 'session_start'
                """, user.getId());
        int activeDays30 = intValue("""
                SELECT COUNT(DISTINCT DATE(created_at)) FROM user_usage_events
                WHERE user_id = ? AND created_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY)
                """, user.getId());
        int sessions30 = intValue("""
                SELECT COUNT(*) FROM user_usage_events
                WHERE user_id = ? AND event_type = 'session_start'
                  AND created_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY)
                """, user.getId());
        List<String> usedFeatures = jdbc.queryForList(
                """
                SELECT DISTINCT feature FROM user_usage_events
                WHERE user_id = ?
                  AND event_type = 'feature_visit'
                  AND feature IS NOT NULL
                  AND feature <> ''
                  AND created_at >= DATE_SUB(NOW(6), INTERVAL 30 DAY)
                """,
                String.class,
                user.getId()
        );

        double frequencyScore = clamp((activeDays30 * 1.5) + (sessions30 * 0.35));
        double coverageScore = clamp((usedFeatures.size() * 10.0) / expectedFeatures(user.getRole()).size());
        double overallScore = round1(clamp((relevanceScore * 0.50) + (frequencyScore * 0.25) + (coverageScore * 0.25)));
        relevanceScore = round1(relevanceScore);
        frequencyScore = round1(frequencyScore);
        coverageScore = round1(coverageScore);

        String featuresUsed = String.join(",", usedFeatures.stream().sorted().toList());
        Timestamp lastSeen = jdbc.queryForObject(
                """
                SELECT MAX(created_at) FROM user_usage_events WHERE user_id = ?
                """,
                Timestamp.class,
                user.getId()
        );
        String rationale = String.format(
                Locale.US,
                "Chat relevance %.1f/10 from %d questions. Frequency %.1f/10 from %d active days and %d sessions in 30 days. Coverage %.1f/10 from %d of %d key features.",
                relevanceScore,
                chatQuestions,
                frequencyScore,
                activeDays30,
                sessions30,
                coverageScore,
                usedFeatures.size(),
                expectedFeatures(user.getRole()).size()
        );

        jdbc.update(
                """
                INSERT INTO user_engagement_scores
                    (user_id, email, overall_score, relevance_score, frequency_score, coverage_score,
                     total_sessions, total_chat_questions, active_days_30, features_used, last_seen_at, rationale, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6))
                ON DUPLICATE KEY UPDATE
                    email = VALUES(email),
                    overall_score = VALUES(overall_score),
                    relevance_score = VALUES(relevance_score),
                    frequency_score = VALUES(frequency_score),
                    coverage_score = VALUES(coverage_score),
                    total_sessions = VALUES(total_sessions),
                    total_chat_questions = VALUES(total_chat_questions),
                    active_days_30 = VALUES(active_days_30),
                    features_used = VALUES(features_used),
                    last_seen_at = VALUES(last_seen_at),
                    rationale = VALUES(rationale),
                    updated_at = NOW(6)
                """,
                user.getId(),
                user.getEmail(),
                overallScore,
                relevanceScore,
                frequencyScore,
                coverageScore,
                totalSessions,
                chatQuestions,
                activeDays30,
                featuresUsed,
                lastSeen,
                rationale
        );

        return scoreCard(user);
    }

    private ScoreCardResponse scoreCard(User user) {
        return jdbc.queryForObject(
                """
                SELECT overall_score, relevance_score, frequency_score, coverage_score,
                       total_sessions, total_chat_questions, active_days_30,
                       features_used, last_seen_at, updated_at, rationale
                FROM user_engagement_scores
                WHERE user_id = ?
                """,
                (rs, row) -> new ScoreCardResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.isEnabled(),
                        rs.getDouble("overall_score"),
                        rs.getDouble("relevance_score"),
                        rs.getDouble("frequency_score"),
                        rs.getDouble("coverage_score"),
                        rs.getInt("total_sessions"),
                        rs.getInt("total_chat_questions"),
                        rs.getInt("active_days_30"),
                        rs.getString("features_used"),
                        instant(rs.getTimestamp("last_seen_at")),
                        instant(rs.getTimestamp("updated_at")),
                        rs.getString("rationale")
                ),
                user.getId()
        );
    }

    private double avgRelevance(Long userId) {
        Double avg = jdbc.queryForObject(
                """
                SELECT AVG(relevance_score) FROM (
                    SELECT relevance_score
                    FROM user_chat_evaluations
                    WHERE user_id = ?
                    ORDER BY created_at DESC
                    LIMIT 50
                ) recent_scores
                """,
                Double.class,
                userId
        );
        return avg == null ? 0.0 : clamp(avg);
    }

    private int intValue(String sql, Long userId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, userId);
        return value == null ? 0 : value;
    }

    private List<String> expectedFeatures(Role role) {
        if (role == Role.ADMIN) return ADMIN_FEATURES;
        if (role == Role.STAFF) return STAFF_FEATURES;
        return DELEGATE_FEATURES;
    }

    private String metadataJson(ScoringEventRequest request) {
        if (request.metadata() == null || request.metadata().isEmpty()) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(request.metadata());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String clean(String value, String fallback, int maxLength) {
        String next = value == null || value.isBlank() ? fallback : value.trim();
        if (next == null) return null;
        return next.length() > maxLength ? next.substring(0, maxLength) : next;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(10.0, value));
    }

    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
