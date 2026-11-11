package com.notare.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Created by the landing page's one-click demo; see V21__add_user_demo_flag.sql.
    @Builder.Default
    @Column(nullable = false)
    private boolean demo = false;

    // See V25__add_password_reset_and_email_verification.sql.
    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    // Tokens issued before this are rejected by JwtAuthenticationFilter.
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // The secret part of this person's calendar feed URL (com.notare.calendar); null until they
    // ask for a link. Resetting it replaces it, so an old link stops working.
    @Column(name = "calendar_token", unique = true, length = 64)
    private String calendarToken;

    // Notifications newer than this are unread; null means never opened (com.notare.notification).
    @Column(name = "notifications_seen_at")
    private java.time.Instant notificationsSeenAt;

    // Comma-separated NotificationKind names this person turned off.
    @Builder.Default
    @Column(name = "muted_notifications", nullable = false)
    private String mutedNotifications = "";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
