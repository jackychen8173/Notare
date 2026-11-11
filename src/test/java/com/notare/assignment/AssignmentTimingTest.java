package com.notare.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Due times in the course's time zone, scheduled opening, closing, late penalties and points.
 * The visibility rule matters most: students can't see, open or submit to an assignment before
 * it opens.
 */
class AssignmentTimingTest extends IntegrationTest {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private JsonNode createAssignment(Account tutor, JsonNode course, Map<String, Object> fields, int status)
            throws Exception {
        Map<String, Object> body = new HashMap<>(fields);
        body.putIfAbsent("title", "Arrays");
        return call(post("/api/courses/" + course.get("id").asText() + "/assignments"), tutor.token(), body, status);
    }

    /**
     * Fails on a JDK whose time zone data predates tzdata 2026b, which would put every BC due time
     * an hour off. Use JDK 21.0.12.1 or later (production runs 21.0.13).
     */
    @Test
    void theJvmKnowsBcIsOnUtcMinus7AllYear() {
        assertThat(LocalDateTime.of(2030, 1, 15, 12, 0).atZone(ZoneId.of("America/Vancouver")).getOffset())
                .as("JVM time zone data is older than tzdata 2026b: update the JDK")
                .isEqualTo(java.time.ZoneOffset.ofHours(-7));
    }

    @Test
    void aDateAloneIsDueAtTheEndOfThatDayInTheCourseTimeZone() throws Exception {
        Account tutor = register("TUTOR");
        JsonNode course = createCourse(tutor);
        assertThat(course.get("timeZone").asText()).isEqualTo("America/Vancouver");

        JsonNode assignment = createAssignment(tutor, course, Map.of("dueDate", "2030-01-15"), 201).get("data");

        // BC has been on UTC-7 all year since 2026-11-01.
        assertThat(assignment.get("dueAt").asText()).isEqualTo("2030-01-15T23:59:00-07:00");
        assertThat(assignment.get("dueDate").asText()).isEqualTo("2030-01-15");
        assertThat(assignment.get("timeZone").asText()).isEqualTo("America/Vancouver");
        assertThat(assignment.get("acceptingSubmissions").asBoolean()).isTrue();
        assertThat(assignment.get("pointsPossible").isNull()).isTrue();
    }

    @Test
    void dueTimesUseTheCourseTimeZoneAndCanBeEdited() throws Exception {
        Account tutor = register("TUTOR");
        JsonNode course = data(post("/api/courses"), tutor.token(),
                Map.of("name", "AP CSA Toronto", "subject", "Computer Science", "timeZone", "America/Toronto"), 201);

        JsonNode assignment = createAssignment(tutor, course,
                Map.of("dueAt", "2030-07-01T09:30", "pointsPossible", 20, "latePenaltyPercent", 10), 201).get("data");
        assertThat(assignment.get("dueAt").asText()).isEqualTo("2030-07-01T09:30:00-04:00");
        assertThat(assignment.get("pointsPossible").asInt()).isEqualTo(20);
        assertThat(assignment.get("latePenaltyPercent").asInt()).isEqualTo(10);

        JsonNode edited = data(put("/api/assignments/" + assignment.get("id").asText()), tutor.token(),
                Map.of("title", "Arrays, part 2", "dueAt", "2030-12-01T15:00", "closesAt", "2030-12-01T15:00",
                        "pointsPossible", 25), 200);
        assertThat(edited.get("title").asText()).isEqualTo("Arrays, part 2");
        // Winter: Toronto is UTC-5. Closing at the due time locks it then.
        assertThat(edited.get("dueAt").asText()).isEqualTo("2030-12-01T15:00:00-05:00");
        assertThat(edited.get("closesAt").asText()).isEqualTo("2030-12-01T15:00:00-05:00");
        assertThat(edited.get("pointsPossible").asInt()).isEqualTo(25);
        // Omitted on a full update means "none".
        assertThat(edited.get("latePenaltyPercent").asInt()).isZero();
    }

    @Test
    void badTimesPointsAndZonesAreRejected() throws Exception {
        Account tutor = register("TUTOR");
        call(post("/api/courses"), tutor.token(),
                Map.of("name", "Bad zone", "subject", "CS", "timeZone", "Mars/Olympus_Mons"), 400);
        call(post("/api/courses"), tutor.token(),
                Map.of("name", "Offset zone", "subject", "CS", "timeZone", "+05:00"), 400);

        JsonNode course = createCourse(tutor);
        createAssignment(tutor, course, Map.of(), 400);
        createAssignment(tutor, course, Map.of("dueAt", "2030-01-15T12:00", "opensAt", "2030-01-15T12:00"), 400);
        createAssignment(tutor, course, Map.of("dueAt", "2030-01-15T12:00", "closesAt", "2030-01-15T11:59"), 400);
        createAssignment(tutor, course, Map.of("dueAt", "2030-01-15T12:00", "pointsPossible", 0), 400);
        createAssignment(tutor, course, Map.of("dueAt", "2030-01-15T12:00", "latePenaltyPercent", 101), 400);
    }

    @Test
    void studentsCannotSeeOrSubmitBeforeItOpens() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        String courseId = course.get("id").asText();

        String later = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(2).format(MINUTES);
        String dueLater = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(9).format(MINUTES);
        String id = createAssignment(tutor, course, Map.of("title", "SCHEDULED", "dueAt", dueLater, "opensAt", later), 201)
                .get("data").get("id").asText();

        // The tutor sees it.
        assertThat(getJson("/api/courses/" + courseId + "/assignments", tutor.token(), 200).get("data")).hasSize(1);

        // The student doesn't, anywhere.
        assertThat(getJson("/api/student/courses/" + courseId + "/assignments", student.token(), 200).get("data"))
                .isEmpty();
        getJson("/api/student/assignments/" + id, student.token(), 404);
        getJson("/api/student/assignments/" + id + "/rubric", student.token(), 404);
        call(post("/api/assignments/" + id + "/submit"), student.token(), Map.of("content", "early"), 404);
        assertThat(getJson("/api/student/home", student.token(), 200).toString()).doesNotContain("SCHEDULED");
        assertThat(getJson("/api/student/courses/" + courseId + "/progress", student.token(), 200).toString())
                .doesNotContain("SCHEDULED");
    }

    @Test
    void aClosedAssignmentRejectsSubmissions() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);

        JsonNode assignment = createAssignment(tutor, course,
                Map.of("dueAt", "2020-01-10T12:00", "closesAt", "2020-01-10T12:00"), 201).get("data");
        assertThat(assignment.get("acceptingSubmissions").asBoolean()).isFalse();

        String id = assignment.get("id").asText();
        // Still visible to the student, but closed.
        assertThat(getJson("/api/student/assignments/" + id, student.token(), 200)
                .get("data").get("acceptingSubmissions").asBoolean()).isFalse();
        call(post("/api/assignments/" + id + "/submit"), student.token(), Map.of("content", "too late"), 409);
    }

    @Test
    void lateWorkIsFlaggedAndThePenaltyApplied() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);

        // Due 30 hours ago: 2 days late (part of a day counts), 10% a day, so 20% off.
        String dueAt = LocalDateTime.now(ZoneId.of("America/Vancouver")).minusHours(30).format(MINUTES);
        UUID assignmentId = UUID.fromString(createAssignment(tutor, course,
                Map.of("dueAt", dueAt, "pointsPossible", 20, "latePenaltyPercent", 10), 201)
                .get("data").get("id").asText());
        UUID submissionId = submit(student, assignmentId, "late work");

        JsonNode mine = getJson("/api/student/assignments/" + assignmentId + "/submission", student.token(), 200)
                .get("data");
        assertThat(mine.get("late").asBoolean()).isTrue();
        assertThat(mine.get("daysLate").asInt()).isEqualTo(2);
        assertThat(mine.get("latePenaltyPercent").asInt()).isEqualTo(20);
        assertThat(mine.get("pointsPossible").asInt()).isEqualTo(20);
        assertThat(mine.get("pointsEarned").isNull()).isTrue();

        JsonNode released = data(patch("/api/submissions/" + submissionId + "/release"), tutor.token(),
                Map.of("tutorFeedback", "Good work", "pointsEarned", 15), 200);
        assertThat(released.get("pointsEarned").decimalValue()).isEqualByComparingTo("15");
        assertThat(released.get("pointsAfterPenalty").decimalValue()).isEqualByComparingTo("12");

        mine = getJson("/api/student/assignments/" + assignmentId + "/submission", student.token(), 200).get("data");
        assertThat(mine.get("pointsEarned").decimalValue()).isEqualByComparingTo("15");
        assertThat(mine.get("pointsAfterPenalty").decimalValue()).isEqualByComparingTo("12");
    }

    @Test
    void onTimeWorkHasNoPenaltyAndNegativePointsAreRejected() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = UUID.fromString(createAssignment(tutor, course,
                Map.of("dueDate", "2030-01-15", "pointsPossible", 10), 201).get("data").get("id").asText());
        UUID submissionId = submit(student, assignmentId, "on time");

        JsonNode tutorView = data(patch("/api/submissions/" + submissionId + "/release"), tutor.token(),
                Map.of("tutorFeedback", "Nice", "pointsEarned", 9), 200);
        assertThat(tutorView.get("late").asBoolean()).isFalse();
        assertThat(tutorView.get("pointsAfterPenalty").decimalValue()).isEqualByComparingTo("9");

        call(patch("/api/submissions/" + submissionId + "/release"), tutor.token(),
                Map.of("tutorFeedback", "Nice", "pointsEarned", -1), 400);
    }
}
