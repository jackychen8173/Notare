package com.notare.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used when no email provider is configured. In local development (email.log-content=true) it logs
 * the plain-text body, so reset and verification links can be clicked from the console. Anywhere
 * else it logs only that an email was skipped, because the body holds a live one-time link.
 */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final boolean logContent;

    public LoggingEmailSender(boolean logContent) {
        this.logContent = logContent;
    }

    @Override
    public void send(String to, String subject, String html, String text) {
        if (logContent) {
            log.info("Email not sent (no provider configured). To: {} | Subject: {}\n{}", to, subject, text);
        } else {
            log.warn("Email not sent: RESEND_API_KEY is not set (subject: {})", subject);
        }
    }
}
