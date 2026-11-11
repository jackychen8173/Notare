package com.notare.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Calendar subscribe links: the token is the only credential, so what matters is that a feed
 * holds exactly what its owner can see, a reset kills the old link, and times are right in UTC.
 */
class CalendarFeedTest extends IntegrationTest {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private String token(Account account) throws Exception {
        return data(post("/api/calendar-feed"), account.token(), null, 200).get("token").asText();
    }

    /** The feed, fetched with no credentials at all, like a calendar app would. */
    private String feed(String token, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(get("/api/calendar-feeds/" + token + ".ics")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        if (expectedStatus == 200) {
            assertThat(result.getResponse().getContentType()).startsWith("text/calendar");
        }
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Unfolds RFC 5545 continuation lines so assertions can match whole properties. */
    private static String unfold(String ics) {
        return ics.replace("\r\n ", "");
    }

    @Test
    void aStudentFeedHasTheirClassesDueTimesAndQuizzesInUtc() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        Account outsider = register("STUDENT");
        JsonNode course = data(post("/api/courses"), tutor.token(), Map.of(
                "name", "AP CSA, Period 3", "subject", "Computer Science",
                "schedule", Map.of("days", List.of("MONDAY", "WEDNESDAY", "FRIDAY"),
                        "startTime", "09:00", "endTime", "10:15")), 201);
        String courseId = course.get("id").asText();
        join(student, course);

        // Vancouver is UTC-7 all year, so 11:59 PM is 06:59 the next day in UTC.
        data(post("/api/courses/" + courseId + "/assignments"), tutor.token(),
                Map.of("title", "Arrays; part 1", "dueAt", "2030-01-15T23:59"), 201);
        String later = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(3).format(MINUTES);
        String muchLater = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(9).format(MINUTES);
        data(post("/api/courses/" + courseId + "/assignments"), tutor.token(),
                Map.of("title", "SCHEDULED", "dueAt", muchLater, "opensAt", later), 201);
        String quizId = data(post("/api/courses/" + courseId + "/quizzes"), tutor.token(),
                Map.of("title", "Unit 1 quiz", "dueAt", "2030-02-01T15:30"), 201).get("id").asText();
        call(post("/api/quizzes/" + quizId + "/questions"), tutor.token(),
                Map.of("type", "TRUE_FALSE", "prompt", "int is primitive", "pointsPossible", 1, "correctBoolean", true), 201);
        call(patch("/api/quizzes/" + quizId + "/publish"), tutor.token(), null, 200);
        data(post("/api/courses/" + courseId + "/quizzes"), tutor.token(),
                Map.of("title", "DRAFT QUIZ", "dueAt", "2030-02-02T15:30"), 201);

        // No link until asked for.
        assertThat(data(get("/api/calendar-feed"), student.token(), null, 200).get("token").isNull()).isTrue();
        String token = token(student);
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        assertThat(data(get("/api/calendar-feed"), student.token(), null, 200).get("token").asText()).isEqualTo(token);

        String ics = unfold(feed(token, 200));
        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("SUMMARY:Due: Arrays\\; part 1\r\n");
        assertThat(ics).contains("DTSTART:20300116T065900Z\r\n");
        assertThat(ics).contains("SUMMARY:Quiz due: Unit 1 quiz\r\n").contains("DTSTART:20300201T223000Z\r\n");
        assertThat(ics).contains("/student/assignments/");
        // Class meetings at 9:00 Vancouver time are 16:00 UTC, 75 minutes long.
        assertThat(ics).contains("SUMMARY:AP CSA\\, Period 3\r\n").containsPattern("DTSTART:\\d{8}T160000Z\r\nDTEND:\\d{8}T171500Z");
        // Not open yet, or not published: not in the student's feed.
        assertThat(ics).doesNotContain("SCHEDULED").doesNotContain("DRAFT QUIZ");

        // An outsider's feed doesn't have the course at all.
        assertThat(feed(token(outsider), 200)).doesNotContain("Period 3").doesNotContain("Arrays");

        // The tutor's feed has the scheduled assignment, but still not the unpublished quiz.
        String tutorIcs = unfold(feed(token(tutor), 200));
        assertThat(tutorIcs).contains("SUMMARY:Due: SCHEDULED").doesNotContain("DRAFT QUIZ");
        assertThat(tutorIcs).doesNotContain("/student/");
    }

    @Test
    void resettingTheLinkKillsTheOldOneAndUnknownTokensAre404() throws Exception {
        Account student = register("STUDENT");
        String first = token(student);
        feed(first, 200);
        String second = token(student);
        assertThat(second).isNotEqualTo(first);
        feed(first, 404);
        feed(second, 200);
        feed("not-a-real-token", 404);
        // Making or reading a link needs a signed-in user.
        call(post("/api/calendar-feed"), null, null, 401);
    }

    @Test
    void quizDueTimesAreSetAndShownInTheCourseTimeZone() throws Exception {
        Account tutor = register("TUTOR");
        JsonNode course = data(post("/api/courses"), tutor.token(),
                Map.of("name", "Toronto section", "subject", "CS", "timeZone", "America/Toronto"), 201);
        JsonNode quiz = data(post("/api/courses/" + course.get("id").asText() + "/quizzes"), tutor.token(),
                Map.of("title", "Q", "dueAt", "2030-07-01T09:30"), 201);
        assertThat(quiz.get("dueAt").asText()).isEqualTo("2030-07-01T09:30:00-04:00");
        assertThat(quiz.get("timeZone").asText()).isEqualTo("America/Toronto");

        JsonNode cleared = data(put("/api/quizzes/" + quiz.get("id").asText()), tutor.token(), Map.of("title", "Q"), 200);
        assertThat(cleared.get("dueAt").isNull()).isTrue();
    }

    @Test
    void textIsEscapedAndLongLinesAreFoldedWithoutSplittingCharacters() {
        assertThat(ICalendar.escape("a,b;c\\d\ne")).isEqualTo("a\\,b\\;c\\\\d\\ne");
        String folded = ICalendar.fold("SUMMARY:" + "é".repeat(60));
        for (String line : folded.split("\r\n")) {
            assertThat(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        assertThat(folded.replace("\r\n ", "")).isEqualTo("SUMMARY:" + "é".repeat(60) + "\r\n");
    }
}
