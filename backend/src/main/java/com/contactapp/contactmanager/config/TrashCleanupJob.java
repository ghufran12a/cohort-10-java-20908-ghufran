package com.contactapp.contactmanager.config;

import com.contactapp.contactmanager.service.ContactService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * An automatic job: permanently removes contacts that have been in the trash for more than 30 days.
 * It runs shortly after the app starts and then once every 24 hours.
 * (A fixed delay is used instead of "every night at 3 am" because a student laptop is often off at night.)
 */
@Configuration
@EnableScheduling
public class TrashCleanupJob {

    private final ContactService contactService;

    public TrashCleanupJob(ContactService contactService) {
        this.contactService = contactService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT24H") // ISO-8601: 1 minute, 24 hours
    public void removeExpiredContacts() {
        contactService.purgeExpired();
    }
}
