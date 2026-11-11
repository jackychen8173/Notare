package com.notare.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Notifications are worked out from what each person can already see, so the checks that matter
 * are the visibility ones: nothing before it opens, nothing from other students' private threads,
 * no names on discussion activity.
 */
class NotificationTest extends IntegrationTest {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static String hoursFromNow(long hours) {
        return LocalDateTime.now(ZoneId.of("America/Vancouver")).plusHours(hours).format(MINUTES);
    }

    private JsonNode notifications(Account account) throws Exception {
        return getJson("/api/notifications", account.token(), 200).get("data");
    }

    private static List<String> titles(JsonNode notifications) {
        List<String> titles = new ArrayList<>();
        notifications.get("items").forEach(item -> titles.add(item.get("kind").asText() + " " + item.get("title").asText()));
        return titles;
    }

    @Test
    void studentsHearAboutNewWorkDueSoonAnnouncementsAndFeedback() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(student, course);

        assertThat(titles(notifications(tutor))).contains("JOINED " + student.name() + " joined");

        UUID soon = UUID.fromString(data(post("/api/courses/" + courseId + "/assignments"), tutor.token(),
                Map.of("title", "Due tonight", "dueAt", hoursFromNow(5)), 201).get("id").asText());
        data(post("/api/courses/" + courseId + "/assignments"), tutor.token(),
                Map.of("title", "SCHEDULED", "dueAt", hoursFromNow(200), "opensAt", hoursFromNow(100)), 201);
        call(post("/api/courses/" + courseId + "/announcements"), tutor.token(), Map.of("content", "Quiz on Friday"), 201);

        JsonNode mine = notifications(student);
        assertThat(titles(mine)).contains("NEW_WORK New assignment: Due tonight", "DUE_SOON Due soon: Due tonight",
                "ANNOUNCEMENT Announcement");
        assertThat(mine.toString()).doesNotContain("SCHEDULED");
        assertThat(mine.get("unreadCount").asInt()).isEqualTo(3);
        assertThat(mine.get("availableKinds")).hasSize(5);

        // Handing it in clears "due soon" and tells the tutor.
        UUID submission = submit(student, soon, "done");
        assertThat(titles(notifications(student))).doesNotContain("DUE_SOON Due soon: Due tonight");
        assertThat(titles(notifications(tutor))).contains("SUBMISSION " + student.name() + " handed in Due tonight");

        // Opening the list marks everything read; something new is unread again.
        call(post("/api/notifications/seen"), student.token(), null, 200);
        assertThat(notifications(student).get("unreadCount").asInt()).isZero();
        call(patch("/api/submissions/" + submission + "/release"), tutor.token(), Map.of("tutorFeedback", "Nice"), 200);
        mine = notifications(student);
        assertThat(titles(mine)).contains("FEEDBACK Feedback returned: Due tonight");
        assertThat(mine.get("unreadCount").asInt()).isEqualTo(1);
    }

    @Test
    void mutedKindsAreLeftOutAndOnlyYourRolesKindsCanBeMuted() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        call(post("/api/courses/" + course.get("id").asText() + "/announcements"), tutor.token(), Map.of("content", "Hi"), 201);

        JsonNode muted = data(put("/api/notifications/settings"), student.token(), Map.of("mutedKinds", List.of("ANNOUNCEMENT")), 200);
        assertThat(muted.get("mutedKinds").toString()).isEqualTo("[\"ANNOUNCEMENT\"]");
        assertThat(titles(muted)).noneMatch(title -> title.startsWith("ANNOUNCEMENT"));
        call(put("/api/notifications/settings"), student.token(), Map.of("mutedKinds", List.of("SUBMISSION")), 400);

        JsonNode unmuted = data(put("/api/notifications/settings"), student.token(), Map.of("mutedKinds", List.of()), 200);
        assertThat(titles(unmuted)).anyMatch(title -> title.startsWith("ANNOUNCEMENT"));
    }

    @Test
    void discussionNotificationsFollowThreadPrivacyAndCarryNoNames() throws Exception {
        Account tutor = register("TUTOR");
        Account ada = register("STUDENT");
        Account bo = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(ada, course);
        join(bo, course);

        String privateThread = data(post("/api/student/courses/" + courseId + "/discussions"), ada.token(),
                Map.of("title", "PRIVATE QUESTION", "body", "Help with loops", "visibility", "PRIVATE"), 201).get("id").asText();
        String publicThread = data(post("/api/student/courses/" + courseId + "/discussions"), ada.token(),
                Map.of("title", "Public thread", "body", "Anyone?", "visibility", "PUBLIC"), 201).get("id").asText();

        assertThat(titles(notifications(tutor))).contains("DISCUSSION New post: PRIVATE QUESTION");

        call(post("/api/discussions/" + privateThread + "/posts"), tutor.token(), Map.of("body", "Sure"), 201);
        call(post("/api/student/discussions/" + publicThread + "/posts"), bo.token(),
                Map.of("body", "Me too", "anonymous", true), 201);

        JsonNode adas = notifications(ada);
        assertThat(titles(adas)).contains("DISCUSSION 1 new reply in your thread: PRIVATE QUESTION",
                "DISCUSSION 1 new reply in your thread: Public thread");
        // Bo replied anonymously: his name isn't anywhere in Ada's notifications.
        assertThat(adas.toString()).doesNotContain(bo.name()).doesNotContain(bo.email());
        // Bo never hears about Ada's private thread.
        assertThat(notifications(bo).toString()).doesNotContain("PRIVATE QUESTION");
    }
}
