package com.postoffice.controller;

import com.postoffice.service.MailService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mock-emails")
public class MockMailController {

    private final MailService mailService;

    public MockMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping
    public List<MailService.MockEmail> getMockEmails() {
        return mailService.getMockEmails();
    }

    @DeleteMapping
    public String clearMockEmails() {
        mailService.clearMockEmails();
        return "Simulated inbox cleared";
    }

    @RequestMapping(value = "/send-test", method = {RequestMethod.GET, RequestMethod.POST})
    public String sendTestEmail(
            @RequestParam(required = false, defaultValue = "debasishdas1708@gmail.com") String to,
            @RequestParam(required = false, defaultValue = "Test Email from PostOffice") String subject,
            @RequestParam(required = false, defaultValue = "This is a test email sent using Resend SDK.") String body) {
        try {
            mailService.sendEmail(to, subject, body);
            return "Test email successfully processed for delivery to: " + to + "\nSubject: " + subject + "\nCheck logs/inbox for delivery status.";
        } catch (Exception e) {
            return "Failed to process test email: " + e.getMessage();
        }
    }
}
