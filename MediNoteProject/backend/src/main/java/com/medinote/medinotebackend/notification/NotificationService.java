package com.medinote.medinotebackend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medinote.medinotebackend.notification.dto.DeviceTokenRequest;
import com.medinote.medinotebackend.notification.dto.NotificationResponse;
import com.medinote.medinotebackend.user.Role;
import com.medinote.medinotebackend.user.User;
import com.medinote.medinotebackend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationService {

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final ObjectMapper mapper;

    public NotificationService(JdbcTemplate jdbc, UserRepository users, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public void registerDeviceToken(String email, DeviceTokenRequest request) {
        if (request.deviceToken() == null || request.deviceToken().isBlank()) {
            throw new IllegalArgumentException("Device token is required");
        }

        User user = user(email);
        jdbc.update(
                """
                INSERT INTO user_device_tokens
                    (user_id, email, platform, device_id, device_token, enabled, created_at, last_seen_at)
                VALUES (?, ?, ?, ?, ?, 1, NOW(6), NOW(6))
                ON DUPLICATE KEY UPDATE
                    user_id = VALUES(user_id),
                    email = VALUES(email),
                    platform = VALUES(platform),
                    device_id = VALUES(device_id),
                    enabled = 1,
                    last_seen_at = NOW(6)
                """,
                user.getId(),
                user.getEmail(),
                clean(request.platform(), "unknown", 40),
                clean(request.deviceId(), null, 160),
                request.deviceToken().trim()
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email, boolean unreadOnly, int limit) {
        User user = user(email);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String unreadClause = unreadOnly ? "AND read_at IS NULL" : "";
        return jdbc.query(
                """
                SELECT id, type, priority, title, body, action_route, metadata_json,
                       read_at, created_at
                FROM user_notifications
                WHERE user_id = ?
                """ + unreadClause + """
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, row) -> new NotificationResponse(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("priority"),
                        rs.getString("title"),
                        rs.getString("body"),
                        rs.getString("action_route"),
                        rs.getString("metadata_json"),
                        rs.getTimestamp("read_at") != null,
                        instant(rs.getTimestamp("read_at")),
                        instant(rs.getTimestamp("created_at"))
                ),
                user.getId(),
                safeLimit
        );
    }

    @Transactional(readOnly = true)
    public int unreadCount(String email) {
        User user = user(email);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_notifications WHERE user_id = ? AND read_at IS NULL",
                Integer.class,
                user.getId()
        );
        return count == null ? 0 : count;
    }

    @Transactional
    public void markRead(String email, Long notificationId) {
        User user = user(email);
        jdbc.update(
                """
                UPDATE user_notifications
                SET read_at = COALESCE(read_at, NOW(6))
                WHERE id = ? AND user_id = ?
                """,
                notificationId,
                user.getId()
        );
    }

    @Transactional
    public void markAllRead(String email) {
        User user = user(email);
        jdbc.update(
                """
                UPDATE user_notifications
                SET read_at = COALESCE(read_at, NOW(6))
                WHERE user_id = ? AND read_at IS NULL
                """,
                user.getId()
        );
    }

    @Transactional
    public int generateBenefitRemindersForAllUsers() {
        int created = 0;
        for (User user : users.findAll()) {
            if (user.isEnabled()) {
                created += generateBenefitReminder(user);
            }
        }
        return created;
    }

    @Transactional
    public int generateBenefitReminderFor(String email) {
        return generateBenefitReminder(user(email));
    }

    private int generateBenefitReminder(User user) {
        if (hasRecentReminder(user.getId())) {
            return 0;
        }

        UserSignal signal = userSignal(user);
        Reminder reminder = pickReminder(user, signal);
        if (reminder == null) {
            return 0;
        }

        createNotification(
                user,
                reminder.type(),
                reminder.priority(),
                reminder.title(),
                reminder.body(),
                reminder.actionRoute(),
                reminder.metadata()
        );
        return 1;
    }

    public void createNotification(
            User user,
            String type,
            String priority,
            String title,
            String body,
            String actionRoute,
            Map<String, Object> metadata
    ) {
        jdbc.update(
                """
                INSERT INTO user_notifications
                    (user_id, email, type, priority, title, body, action_route, metadata_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(),
                user.getEmail(),
                clean(type, "REMINDER", 80),
                clean(priority, "NORMAL", 20),
                clean(title, "MediNote reminder", 180),
                clean(body, "", 700),
                clean(actionRoute, null, 120),
                metadataJson(metadata)
        );

        // Real push delivery goes here once FCM credentials are configured.
        // The notification is already durable and visible in app/web.
    }

    private Reminder pickReminder(User user, UserSignal signal) {
        if (signal.daysSinceSeen() >= 3) {
            return new Reminder(
                    "COME_BACK",
                    "HIGH",
                    "Your workspace is waiting",
                    "Open MediNote for two minutes to review your route, reports, and latest CRM insights.",
                    "dashboard",
                    Map.of("daysSinceSeen", signal.daysSinceSeen())
            );
        }
        if (!signal.featuresUsed().contains("chat")) {
            return new Reminder(
                    "CHAT_VALUE",
                    "NORMAL",
                    "Let the chatbot save you time",
                    "Ask MediNote AI about visits, orders, doctors, products, or sales before your next action.",
                    "chat",
                    Map.of("missingFeature", "chat")
            );
        }
        if (!signal.featuresUsed().contains("report_analysis")) {
            return new Reminder(
                    "REPORT_VALUE",
                    "NORMAL",
                    "Turn reports into clean notes",
                    "Upload a PDF or image in Report Analysis to check quality, missing fields, and reformulated text.",
                    "report",
                    Map.of("missingFeature", "report_analysis")
            );
        }
        if (signal.overallScore() > 0 && signal.overallScore() < 6) {
            return new Reminder(
                    "SCORE_COACH",
                    "NORMAL",
                    "Small actions can raise your score",
                    String.format(Locale.US, "Your usage score is %.1f/10. Try one chat question and one report analysis today.", signal.overallScore()),
                    "scores",
                    Map.of("score", signal.overallScore())
            );
        }
        if (user.getRole() == Role.ADMIN) {
            return new Reminder(
                    "ADMIN_REVIEW",
                    "NORMAL",
                    "Review team adoption",
                    "Check user scores to spot who needs help getting more value from MediNote.",
                    "scores",
                    Map.of("role", "ADMIN")
            );
        }
        if (user.getRole() == Role.STAFF) {
            return new Reminder(
                    "STAFF_INSIGHT",
                    "LOW",
                    "Find the trend behind today",
                    "Use MediNote AI to compare delegate activity, product performance, or order trends.",
                    "chat",
                    Map.of("role", "STAFF")
            );
        }
        return new Reminder(
                "DELEGATE_ROUTE",
                "LOW",
                "Make the next visit sharper",
                "Before your route, ask MediNote AI for visit priorities or product talking points.",
                "chat",
                Map.of("role", "DELEGATE")
        );
    }

    private boolean hasRecentReminder(Long userId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM user_notifications
                WHERE user_id = ?
                  AND type IN ('COME_BACK','CHAT_VALUE','REPORT_VALUE','SCORE_COACH','ADMIN_REVIEW','STAFF_INSIGHT','DELEGATE_ROUTE')
                  AND created_at >= DATE_SUB(NOW(6), INTERVAL 20 HOUR)
                """,
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    private UserSignal userSignal(User user) {
        Map<String, Object> score = jdbc.query(
                """
                SELECT overall_score, total_chat_questions, features_used, last_seen_at
                FROM user_engagement_scores
                WHERE user_id = ?
                """,
                rs -> rs.next()
                        ? Map.of(
                                "overall_score", rs.getDouble("overall_score"),
                                "total_chat_questions", rs.getInt("total_chat_questions"),
                                "features_used", rs.getString("features_used") == null ? "" : rs.getString("features_used"),
                                "last_seen_at", rs.getTimestamp("last_seen_at") == null ? Timestamp.from(Instant.EPOCH) : rs.getTimestamp("last_seen_at")
                        )
                        : Map.of(
                                "overall_score", 0.0,
                                "total_chat_questions", 0,
                                "features_used", "",
                                "last_seen_at", Timestamp.from(Instant.EPOCH)
                        ),
                user.getId()
        );

        String featuresRaw = score.get("features_used").toString();
        List<String> features = featuresRaw.isBlank()
                ? List.of()
                : List.of(featuresRaw.split(","));
        Timestamp lastSeen = (Timestamp) score.get("last_seen_at");
        long daysSinceSeen = Math.max(0, (Instant.now().toEpochMilli() - lastSeen.toInstant().toEpochMilli()) / 86_400_000);
        if (lastSeen.toInstant().equals(Instant.EPOCH)) {
            daysSinceSeen = 99;
        }

        return new UserSignal(
                ((Number) score.get("overall_score")).doubleValue(),
                ((Number) score.get("total_chat_questions")).intValue(),
                features,
                daysSinceSeen
        );
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    private String metadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String clean(String value, String fallback, int maxLength) {
        String next = value == null || value.isBlank() ? fallback : value.trim();
        if (next == null) return null;
        return next.length() > maxLength ? next.substring(0, maxLength) : next;
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record UserSignal(
            double overallScore,
            int totalChatQuestions,
            List<String> featuresUsed,
            long daysSinceSeen
    ) {}

    private record Reminder(
            String type,
            String priority,
            String title,
            String body,
            String actionRoute,
            Map<String, Object> metadata
    ) {}
}
