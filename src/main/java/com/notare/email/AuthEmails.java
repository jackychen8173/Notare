package com.notare.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ExecutorService;

/** The account emails: password reset and email verification. Sent off the request thread. */
@Component
public class AuthEmails {

    private final EmailSender sender;
    private final ExecutorService executor;
    private final String appBaseUrl;

    public AuthEmails(
            EmailSender sender,
            ExecutorService emailExecutor,
            @Value("${email.app-base-url}") String appBaseUrl
    ) {
        this.sender = sender;
        this.executor = emailExecutor;
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    public void sendPasswordReset(String to, String name, String token) {
        String link = link("/reset-password", token);
        send(to, "Reset your Notare password",
                "Hi " + name + ",",
                "Someone asked to reset the password for your Notare account. If it was you, choose a new password here:",
                "Choose a new password", link,
                "The link works once and expires in 30 minutes. If you didn't ask for this, you can ignore this email; your password stays the same.");
    }

    public void sendEmailVerification(String to, String name, String token) {
        String link = link("/verify-email", token);
        send(to, "Confirm your email for Notare",
                "Hi " + name + ",",
                "Confirm this is your email address so Notare can send you password resets and class updates:",
                "Confirm my email", link,
                "The link expires in 3 days. If you didn't create a Notare account, you can ignore this email.");
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(appBaseUrl + path).queryParam("token", token).build().toUriString();
    }

    private void send(String to, String subject, String greeting, String intro, String button, String link, String outro) {
        String text = greeting + "\n\n" + intro + "\n\n" + link + "\n\n" + outro + "\n\nNotare";
        String html = """
                <div style="font-family:Inter,Segoe UI,Arial,sans-serif;font-size:15px;line-height:1.6;color:#1c1c1a;max-width:520px">
                  <p>%s</p>
                  <p>%s</p>
                  <p><a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;font-weight:600">%s</a></p>
                  <p style="color:#5f5e5a;font-size:13px">Or paste this link into your browser:<br>%s</p>
                  <p style="color:#5f5e5a;font-size:13px">%s</p>
                  <p style="color:#5f5e5a;font-size:13px">Notare</p>
                </div>
                """.formatted(
                HtmlUtils.htmlEscape(greeting), HtmlUtils.htmlEscape(intro), HtmlUtils.htmlEscape(link),
                HtmlUtils.htmlEscape(button), HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(outro));
        executor.execute(() -> sender.send(to, subject, html, text));
    }
}
