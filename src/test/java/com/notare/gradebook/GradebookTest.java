package com.notare.gradebook;

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
 * The gradebook grid, course grades and a student's own grades page. What matters most: only
 * released scores count or show, and a student only ever sees their own work.
 */
class GradebookTest extends IntegrationTest {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static String hoursFromNow(long hours) {
        return LocalDateTime.now(ZoneId.of("America/Vancouver")).plusHours(hours).format(MINUTES);
    }

    private String category(Account tutor, String courseId, String name, int weight) throws Exception {
        return data(post("/api/courses/" + courseId + "/grade-categories"), tutor.token(),
                Map.of("name", name, "weightPercent", weight), 201).get("id").asText();
    }

    private UUID assignment(Account tutor, String courseId, String title, String dueAt, Integer points,
                            String categoryId, Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new HashMap<>(extra);
        body.put("title", title);
        body.put("dueAt", dueAt);
        if (points != null) {
            body.put("pointsPossible", points);
        }
        if (categoryId != null) {
            body.put("gradeCategoryId", categoryId);
        }
        return UUID.fromString(data(post("/api/courses/" + courseId + "/assignments"), tutor.token(), body, 201)
                .get("id").asText());
    }

    private void release(Account tutor, UUID submissionId, int points) throws Exception {
        call(patch("/api/submissions/" + submissionId + "/release"), tutor.token(),
                Map.of("tutorFeedback", "Thanks", "pointsEarned", points), 200);
    }

    private JsonNode gradebook(Account tutor, String courseId) throws Exception {
        return getJson("/api/courses/" + courseId + "/gradebook", tutor.token(), 200).get("data");
    }

    private static int column(JsonNode book, String title) {
        JsonNode columns = book.get("columns");
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).get("title").asText().equals(title)) {
                return i;
            }
        }
        throw new AssertionError("No column " + title);
    }

    private static JsonNode row(JsonNode book, Account student) {
        for (JsonNode row : book.get("students")) {
            if (row.get("studentId").asText().equals(student.id().toString())) {
                return row;
            }
        }
        throw new AssertionError("No row for " + student.email());
    }

    @Test
    void weightedCategoriesAddUpToACourseGradeFromReleasedWorkOnly() throws Exception {
        Account tutor = register("TUTOR");
        Account ada = register("STUDENT");
        Account bo = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(ada, course);
        join(bo, course);

        String labs = category(tutor, courseId, "Labs", 60);
        String tests = category(tutor, courseId, "Tests", 40);
        String later = hoursFromNow(48);
        UUID lab1 = assignment(tutor, courseId, "Lab 1", later, 10, labs, Map.of());
        UUID lab2 = assignment(tutor, courseId, "Lab 2", later, 10, labs, Map.of());
        UUID test1 = assignment(tutor, courseId, "Test 1", later, 20, tests, Map.of());
        UUID extra = assignment(tutor, courseId, "Uncategorized", later, 10, null, Map.of());
        assignment(tutor, courseId, "Overdue", hoursFromNow(-2), 10, labs, Map.of());

        release(tutor, submit(ada, lab1, "a"), 8);
        submit(ada, lab2, "b"); // handed in, not released: doesn't count yet
        release(tutor, submit(ada, test1, "c"), 10);
        release(tutor, submit(ada, extra, "d"), 10); // outside every category: doesn't count
        release(tutor, submit(bo, lab1, "e"), 6);

        JsonNode book = gradebook(tutor, courseId);
        assertThat(book.get("categories")).hasSize(2);
        assertThat(book.get("columns").get(column(book, "Uncategorized")).get("countsTowardGrade").asBoolean()).isFalse();
        assertThat(book.get("columns").get(column(book, "Lab 1")).get("averagePercent").decimalValue())
                .isEqualByComparingTo("70.0");

        JsonNode adaRow = row(book, ada);
        assertThat(adaRow.get("email").asText()).isEqualTo(ada.email());
        JsonNode lab2Cell = adaRow.get("cells").get(column(book, "Lab 2"));
        assertThat(lab2Cell.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(lab2Cell.get("points").isNull()).isTrue();
        assertThat(adaRow.get("cells").get(column(book, "Overdue")).get("status").asText()).isEqualTo("MISSING");
        // Labs 8/10 = 80%, Tests 10/20 = 50%; 80 * 0.6 + 50 * 0.4 = 68.
        assertThat(adaRow.get("categoryPercents").get(0).decimalValue()).isEqualByComparingTo("80.0");
        assertThat(adaRow.get("categoryPercents").get(1).decimalValue()).isEqualByComparingTo("50.0");
        assertThat(adaRow.get("coursePercent").decimalValue()).isEqualByComparingTo("68.0");

        // Only Labs graded for Bo: the Tests weight drops out instead of counting as zero.
        JsonNode boRow = row(book, bo);
        assertThat(boRow.get("categoryPercents").get(1).isNull()).isTrue();
        assertThat(boRow.get("coursePercent").decimalValue()).isEqualByComparingTo("60.0");
    }

    @Test
    void withoutCategoriesItIsPointsOverPointsAfterLatePenalties() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(student, course);

        // Due 30 hours ago at 10% a day: 2 days late, 20% off, so 15 becomes 12.
        UUID late = assignment(tutor, courseId, "Late", hoursFromNow(-30), 20, null, Map.of("latePenaltyPercent", 10));
        UUID onTime = assignment(tutor, courseId, "On time", hoursFromNow(48), 30, null, Map.of());
        assignment(tutor, courseId, "No points", hoursFromNow(48), null, null, Map.of());
        release(tutor, submit(student, late, "late"), 15);
        release(tutor, submit(student, onTime, "ok"), 30);

        JsonNode book = gradebook(tutor, courseId);
        JsonNode cell = row(book, student).get("cells").get(column(book, "Late"));
        assertThat(cell.get("status").asText()).isEqualTo("RETURNED");
        assertThat(cell.get("late").asBoolean()).isTrue();
        assertThat(cell.get("daysLate").asInt()).isEqualTo(2);
        assertThat(cell.get("pointsBeforePenalty").decimalValue()).isEqualByComparingTo("15");
        assertThat(cell.get("points").decimalValue()).isEqualByComparingTo("12");
        assertThat(book.get("columns").get(column(book, "No points")).get("countsTowardGrade").asBoolean()).isFalse();
        // (12 + 30) / (20 + 30) = 84%.
        assertThat(row(book, student).get("coursePercent").decimalValue()).isEqualByComparingTo("84.0");
    }

    @Test
    void aResubmissionWaitingForReviewKeepsTheEarlierScore() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(student, course);

        UUID id = assignment(tutor, courseId, "Redo", hoursFromNow(48), 10, null, Map.of("allowResubmission", true));
        release(tutor, submit(student, id, "v1"), 6);
        UUID second = submit(student, id, "v2");

        JsonNode book = gradebook(tutor, courseId);
        JsonNode cell = row(book, student).get("cells").get(column(book, "Redo"));
        assertThat(cell.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(cell.get("points").decimalValue()).isEqualByComparingTo("6");
        assertThat(cell.get("workId").asText()).isEqualTo(second.toString());

        release(tutor, second, 9);
        cell = row(gradebook(tutor, courseId), student).get("cells").get(column(book, "Redo"));
        assertThat(cell.get("status").asText()).isEqualTo("RETURNED");
        assertThat(cell.get("points").decimalValue()).isEqualByComparingTo("9");
    }

    @Test
    void releasedQuizzesCountWithTheirQuestionPoints() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(student, course);

        String quizId = data(post("/api/courses/" + courseId + "/quizzes"), tutor.token(),
                Map.of("title", "Quiz 1"), 201).get("id").asText();
        // Adding a question returns the whole quiz.
        JsonNode first = data(post("/api/quizzes/" + quizId + "/questions"), tutor.token(),
                Map.of("type", "TRUE_FALSE", "prompt", "int is primitive", "pointsPossible", 4, "correctBoolean", true), 201)
                .get("questions").get(0);
        data(post("/api/quizzes/" + quizId + "/questions"), tutor.token(),
                Map.of("type", "TRUE_FALSE", "prompt", "String is primitive", "pointsPossible", 6, "correctBoolean", false), 201);
        call(patch("/api/quizzes/" + quizId + "/publish"), tutor.token(), null, 200);

        String trueOption = null;
        for (JsonNode option : first.get("options")) {
            if (option.get("text").asText().equals("True")) {
                trueOption = option.get("id").asText();
            }
        }
        String attemptId = data(post("/api/student/quizzes/" + quizId + "/attempt"), student.token(), null, 201)
                .get("id").asText();
        call(put("/api/student/quiz-attempts/" + attemptId + "/answers/" + first.get("id").asText()), student.token(),
                Map.of("selectedOptionId", trueOption), 200);
        call(post("/api/student/quiz-attempts/" + attemptId + "/submit"), student.token(), null, 200);

        JsonNode book = gradebook(tutor, courseId);
        int quiz = column(book, "Quiz 1");
        assertThat(book.get("columns").get(quiz).get("kind").asText()).isEqualTo("QUIZ");
        assertThat(book.get("columns").get(quiz).get("pointsPossible").decimalValue()).isEqualByComparingTo("10");
        assertThat(row(book, student).get("cells").get(quiz).get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(row(book, student).get("cells").get(quiz).get("points").isNull()).isTrue();
        // Not released: the student sees no score either.
        assertThat(getJson("/api/student/courses/" + courseId + "/grades", student.token(), 200)
                .get("data").get("coursePercent").isNull()).isTrue();

        call(patch("/api/quiz-attempts/" + attemptId + "/release"), tutor.token(), null, 200);
        book = gradebook(tutor, courseId);
        assertThat(row(book, student).get("cells").get(quiz).get("points").decimalValue()).isEqualByComparingTo("4");
        assertThat(row(book, student).get("coursePercent").decimalValue()).isEqualByComparingTo("40.0");
    }

    @Test
    void aStudentSeesOnlyTheirOwnGradesForWorkThatHasOpened() throws Exception {
        Account tutor = register("TUTOR");
        Account ada = register("STUDENT");
        Account bo = register("STUDENT");
        Account outsider = register("STUDENT");
        Account otherTutor = register("TUTOR");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(ada, course);
        join(bo, course);

        UUID lab = assignment(tutor, courseId, "Lab", hoursFromNow(48), 10, null, Map.of());
        assignment(tutor, courseId, "SCHEDULED", hoursFromNow(200), 10, null, Map.of("opensAt", hoursFromNow(100)));
        release(tutor, submit(ada, lab, "ada's"), 7);
        release(tutor, submit(bo, lab, "bo's"), 3);

        JsonNode mine = getJson("/api/student/courses/" + courseId + "/grades", ada.token(), 200).get("data");
        assertThat(mine.toString()).doesNotContain("SCHEDULED").doesNotContain(bo.email()).doesNotContain(bo.name());
        assertThat(mine.get("items")).hasSize(1);
        JsonNode item = mine.get("items").get(0);
        assertThat(item.get("cell").get("points").decimalValue()).isEqualByComparingTo("7");
        // No class average: in a small class it would give away classmates' scores.
        assertThat(item.get("column").get("averagePercent").isNull()).isTrue();
        assertThat(mine.get("coursePercent").decimalValue()).isEqualByComparingTo("70.0");

        // The tutor sees the scheduled one too.
        assertThat(gradebook(tutor, courseId).get("columns")).hasSize(2);

        getJson("/api/student/courses/" + courseId + "/grades", outsider.token(), 404);
        getJson("/api/courses/" + courseId + "/gradebook", otherTutor.token(), 404);
        getJson("/api/courses/" + courseId + "/gradebook", ada.token(), 403);
    }
}
