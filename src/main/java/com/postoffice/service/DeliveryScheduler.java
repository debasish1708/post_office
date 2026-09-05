package com.postoffice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryScheduler {

    private final LetterService letterService;

    public DeliveryScheduler(LetterService letterService) {
        this.letterService = letterService;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Kolkata")
    public void dailyNodeHops() {
        letterService.runDailyHops();
    }

    @Scheduled(fixedRate = 600000)
    public void superfastWatch() {
        letterService.completeDueSuperfast();
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Kolkata")
    public void monthlyWalletReset() {
        letterService.resetMonthlyWallets();
    }
}
