package com.postoffice.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class MailService {

    @Value("${resend.api.key:}")
    private String apiKey;

    private Resend resend;

    // Simple thread-safe list to hold mock emails for UI visualization
    private static final List<MockEmail> mockEmails = Collections.synchronizedList(new ArrayList<>());

    public static class MockEmail {
        private String to;
        private String subject;
        private String body;
        private LocalDateTime timestamp;

        public MockEmail(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
            this.timestamp = LocalDateTime.now();
        }

        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getBody() { return body; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            this.resend = new Resend(apiKey.trim());
            System.out.println("Resend: SDK client initialized successfully.");
        } else {
            System.err.println("Resend Warn: 'resend.api.key' is not set or empty. Emails will be logged only.");
        }
    }

    public void sendEmail(String to, String subject, String body) {
        // 1. Log to system console
        System.out.println("\n==========================================");
        System.out.println("SMTP EMAIL OUTBOX [SIMULATED]");
        System.out.println("Timestamp: " + LocalDateTime.now());
        System.out.println("Recipient: " + to);
        System.out.println("Subject  : " + subject);
        System.out.println("Message  :\n" + body);
        System.out.println("==========================================\n");

        // 2. Add to mock in-memory ledger
        mockEmails.add(new MockEmail(to, subject, body));

        // 3. Attempt to send actual email via Resend
        if (resend != null) {
            try {
                String htmlBody = body.replace("\r\n", "<br>").replace("\n", "<br>");
                CreateEmailOptions params = CreateEmailOptions.builder()
                        .from("Post Office <onboarding@resend.dev>")
                        .to(to)
                        .subject(subject)
                        .html(htmlBody)
                        .build();

                CreateEmailResponse response = resend.emails().send(params);
                System.out.println("Resend: Real email dispatched successfully. Response ID: " + response.getId());
            } catch (Exception e) {
                System.err.println("Resend Warn: Real email dispatch failed: " + e.getMessage());
            }
        }
    }

    public List<MockEmail> getMockEmails() {
        return new ArrayList<>(mockEmails);
    }

    public void clearMockEmails() {
        mockEmails.clear();
    }
}
