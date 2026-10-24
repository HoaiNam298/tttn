package com.project.shopapp.components;

import com.project.shopapp.services.AccountSettingsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountIdentitySyncJob {
    private final AccountSettingsService settings;

    public AccountIdentitySyncJob(AccountSettingsService settings) {
        this.settings = settings;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void synchronizePending() {
        settings.synchronizePending();
    }
}
