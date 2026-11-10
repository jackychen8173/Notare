package com.notare.email;

/** Sends one transactional email. Implementations must not throw for delivery failures; they log. */
public interface EmailSender {

    void send(String to, String subject, String html, String text);
}
