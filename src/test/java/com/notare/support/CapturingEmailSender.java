package com.notare.support;

import com.notare.email.EmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Collects the emails the app sends, so tests can follow the links in them. */
public class CapturingEmailSender implements EmailSender {

    public record SentEmail(String to, String subject, String html, String text) {
        private static final Pattern TOKEN = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)");

        public String token() {
            Matcher matcher = TOKEN.matcher(text);
            if (!matcher.find()) {
                throw new AssertionError("No token link in email: " + text);
            }
            return matcher.group(1);
        }
    }

    private final BlockingQueue<SentEmail> sent = new LinkedBlockingQueue<>();

    @Override
    public void send(String to, String subject, String html, String text) {
        sent.add(new SentEmail(to, subject, html, text));
    }

    /** Waits for the next email to {@code to} (emails go out on a background thread). */
    public SentEmail next(String to, String subjectContains) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            SentEmail email = sent.poll(100, TimeUnit.MILLISECONDS);
            if (email != null && email.to().equals(to) && email.subject().contains(subjectContains)) {
                return email;
            }
        }
        throw new AssertionError("No email to " + to + " with subject containing \"" + subjectContains + "\"");
    }

    /** True if no email to {@code to} arrives within a short wait. */
    public boolean noneTo(String to) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(800);
        while (System.nanoTime() < deadline) {
            SentEmail email = sent.poll(100, TimeUnit.MILLISECONDS);
            if (email != null && email.to().equals(to)) {
                return false;
            }
        }
        return true;
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }
}
