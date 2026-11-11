package com.notare.calendar;

import com.notare.common.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class CalendarFeedController {

    private static final MediaType TEXT_CALENDAR = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final CalendarFeedService calendarFeedService;

    public CalendarFeedController(CalendarFeedService calendarFeedService) {
        this.calendarFeedService = calendarFeedService;
    }

    /** The signed-in person's calendar link token, or null if they haven't made one. */
    @GetMapping("/api/calendar-feed")
    public ResponseEntity<ApiResponse<CalendarFeedResponse>> current(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(new CalendarFeedResponse(
                calendarFeedService.currentToken(authentication.getName()))));
    }

    /** Makes a link, or replaces it so the old one stops working. */
    @PostMapping("/api/calendar-feed")
    public ResponseEntity<ApiResponse<CalendarFeedResponse>> reset(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(new CalendarFeedResponse(
                calendarFeedService.resetToken(authentication.getName()))));
    }

    /**
     * The feed itself, public: the token is the credential. Normally reached through the
     * frontend's cached /api/calendar/{token}.ics route rather than directly.
     */
    @GetMapping("/api/calendar-feeds/{token}")
    public ResponseEntity<String> feed(@PathVariable String token) {
        String bare = token.endsWith(".ics") ? token.substring(0, token.length() - 4) : token;
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Robots-Tag", "noindex")
                .body(calendarFeedService.feed(bare));
    }

    public record CalendarFeedResponse(String token) {
    }
}
