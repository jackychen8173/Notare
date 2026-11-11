package com.notare.assignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * How late a submission was and what that costs. Any part of a day counts as a whole day, so work
 * handed in a minute late is 1 day late. The penalty never takes off more than 100%.
 */
public record Lateness(boolean late, int daysLate, int penaltyPercent) {

    private static final long DAY_SECONDS = Duration.ofDays(1).toSeconds();

    public static Lateness of(Assignment assignment, Instant submittedAt) {
        if (!submittedAt.isAfter(assignment.getDueAt())) {
            return new Lateness(false, 0, 0);
        }
        long secondsLate = Duration.between(assignment.getDueAt(), submittedAt).toSeconds();
        int days = (int) Math.min(Integer.MAX_VALUE, (secondsLate + DAY_SECONDS - 1) / DAY_SECONDS);
        int penalty = (int) Math.min(100L, (long) days * assignment.getLatePenaltyPercent());
        return new Lateness(true, days, penalty);
    }

    /** Points after the late penalty, or null when there are no points to take it from. */
    public BigDecimal apply(BigDecimal pointsEarned) {
        if (pointsEarned == null) {
            return null;
        }
        return pointsEarned
                .multiply(BigDecimal.valueOf(100 - penaltyPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
