package com.notare.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Sends through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email). */
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final RestClient client;
    private final String from;

    public ResendEmailSender(String apiKey, String from) {
        this.client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String html, String text) {
        try {
            client.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", from, "to", List.of(to), "subject", subject, "html", html, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            // The recipient isn't logged: it's personal data, and the subject says which flow failed.
            log.error("Email delivery failed (subject: {})", subject, e);
        }
    }
}
