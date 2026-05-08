package com.medinote.medinotebackend.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final NotificationService service;

    public NotificationScheduler(NotificationService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 30 9,16 * * *")
    public void generateBenefitReminders() {
        service.generateBenefitRemindersForAllUsers();
    }
}
