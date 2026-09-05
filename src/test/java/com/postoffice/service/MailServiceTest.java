package com.postoffice.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MailServiceTest {

    @Test
    public void testMockEmailsRecording() {
        MailService mailService = new MailService();
        mailService.clearMockEmails();

        mailService.sendEmail("test@example.com", "Test Subject", "Test Body");

        List<MailService.MockEmail> mockEmails = mailService.getMockEmails();
        assertEquals(1, mockEmails.size());

        MailService.MockEmail email = mockEmails.get(0);
        assertEquals("test@example.com", email.getTo());
        assertEquals("Test Subject", email.getSubject());
        assertEquals("Test Body", email.getBody());
        assertNotNull(email.getTimestamp());
    }

    @Test
    public void testInitWithoutApiKey() {
        MailService mailService = new MailService();
        // Since apiKey is empty or null, resend client should remain null
        mailService.init();
        
        // Asserting that sendEmail can be called without throwing NPE when resend is null
        assertDoesNotThrow(() -> mailService.sendEmail("test@example.com", "Test", "Body"));
    }
}
