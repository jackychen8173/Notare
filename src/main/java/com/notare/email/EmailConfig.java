package com.notare.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class EmailConfig {

    @Bean
    public EmailSender emailSender(
            @Value("${email.resend-api-key:}") String resendApiKey,
            @Value("${email.from}") String from,
            @Value("${email.log-content:false}") boolean logContent
    ) {
        if (resendApiKey.isBlank()) {
            return new LoggingEmailSender(logContent);
        }
        return new ResendEmailSender(resendApiKey, from);
    }

    /**
     * Emails go out off the request thread, so a password-reset request takes the same time whether
     * or not the account exists (no timing hint about which emails are registered). Two threads is
     * plenty for this volume.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService emailExecutor() {
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "email");
            thread.setDaemon(true);
            return thread;
        });
    }
}
