package com.medinote.medinotebackend.notification;

import com.medinote.medinotebackend.auth.MessageResponse;
import com.medinote.medinotebackend.notification.dto.DeviceTokenRequest;
import com.medinote.medinotebackend.notification.dto.NotificationResponse;
import com.medinote.medinotebackend.notification.dto.UnreadCountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/device-token")
    public ResponseEntity<MessageResponse> registerDeviceToken(
            @RequestBody DeviceTokenRequest request,
            Authentication authentication
    ) {
        service.registerDeviceToken(authentication.getName(), request);
        return ResponseEntity.ok(new MessageResponse("Device token registered"));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "30") int limit,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.list(authentication.getName(), unreadOnly, limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(new UnreadCountResponse(service.unreadCount(authentication.getName())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.markRead(authentication.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(Authentication authentication) {
        service.markAllRead(authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Notifications marked as read"));
    }

    @PostMapping("/generate-mine")
    public ResponseEntity<MessageResponse> generateMine(Authentication authentication) {
        int created = service.generateBenefitReminderFor(authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Generated " + created + " reminder(s)"));
    }

    @PostMapping("/admin/generate-reminders")
    public ResponseEntity<MessageResponse> generateAll() {
        int created = service.generateBenefitRemindersForAllUsers();
        return ResponseEntity.ok(new MessageResponse("Generated " + created + " reminder(s)"));
    }
}
