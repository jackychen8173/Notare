package com.notare.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * A minimal iCalendar (RFC 5545) writer: one VCALENDAR of VEVENTs. Every time is written in UTC
 * ("...Z"), converted in Java with the course's time zone, so a calendar app with out-of-date time
 * zone rules can't shift it (BC moved to permanent UTC-7 in Nov 2026). For the same reason weekly
 * class meetings are written as separate events, not one event with a recurrence rule.
 */
final class ICalendar {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAX_LINE_OCTETS = 75;

    private final StringBuilder out = new StringBuilder();
    private final String stamp;

    ICalendar(String name, String description, Instant now) {
        this.stamp = UTC.format(now);
        line("BEGIN:VCALENDAR");
        line("VERSION:2.0");
        line("PRODID:-//Notare//Calendar feed//EN");
        line("CALSCALE:GREGORIAN");
        line("METHOD:PUBLISH");
        line("X-WR-CALNAME:" + escape(name));
        line("X-WR-CALDESC:" + escape(description));
        // A hint only: Google ignores it, Apple and Outlook may poll this often.
        line("REFRESH-INTERVAL;VALUE=DURATION:PT6H");
        line("X-PUBLISHED-TTL:PT6H");
    }

    /** end null means a moment (a due time), which calendars show as a zero-length event. */
    ICalendar event(String uid, Instant start, Instant end, String summary, String description, String url) {
        line("BEGIN:VEVENT");
        line("UID:" + escape(uid));
        line("DTSTAMP:" + stamp);
        line("DTSTART:" + UTC.format(start));
        if (end != null) {
            line("DTEND:" + UTC.format(end));
        }
        line("SUMMARY:" + escape(summary));
        if (description != null && !description.isBlank()) {
            line("DESCRIPTION:" + escape(description));
        }
        if (url != null) {
            line("URL:" + url);
        }
        line("TRANSP:" + (end == null ? "TRANSPARENT" : "OPAQUE"));
        line("END:VEVENT");
        return this;
    }

    String build() {
        line("END:VCALENDAR");
        return out.toString();
    }

    /** TEXT values: backslash, semicolon, comma and line breaks are escaped (RFC 5545 3.3.11). */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /**
     * Folds a content line at 75 octets with CRLF + space (RFC 5545 3.1), never splitting a UTF-8
     * character, and ends it with CRLF.
     */
    static String fold(String line) {
        StringBuilder folded = new StringBuilder();
        int octets = 0;
        int i = 0;
        while (i < line.length()) {
            int codePoint = line.codePointAt(i);
            String character = new String(Character.toChars(codePoint));
            int size = character.getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_LINE_OCTETS) {
                folded.append("\r\n ");
                octets = 1; // the leading space counts toward the next line
            }
            folded.append(character);
            octets += size;
            i += Character.charCount(codePoint);
        }
        return folded.append("\r\n").toString();
    }

    private void line(String content) {
        out.append(fold(content));
    }
}
