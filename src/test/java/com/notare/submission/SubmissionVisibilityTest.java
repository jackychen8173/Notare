package com.notare.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The trust boundary for students: nothing Sage drafts and nothing the tutor writes reaches the
 * student until the tutor releases the submission, and Sage's SUGGESTED line comments never do.
 */
class SubmissionVisibilityTest extends IntegrationTest {

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    SubmissionLineCommentRepository lineCommentRepository;

    private static final String CODE = """
            public class Main {
                public static void main(String[] args) {
                    for (int i = 0; i <= 10; i++) System.out.println(i);
                }
            }""";

    @Test
    void feedbackAndLineCommentsStayHiddenUntilRelease() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = createAssignment(tutor, course);
        UUID submissionId = submit(student, assignmentId, CODE);

        // Sage's draft review and a Sage line suggestion, as a Sage review would leave them.
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        submission.setSageFeedback("""
                {"correctness": "SAGE-DRAFT: the loop runs one time too many.", "style": "", \
                "suggestions": "", "encouragement": ""}""");
        submissionRepository.save(submission);
        lineCommentRepository.save(SubmissionLineComment.builder()
                .submission(submission).lineNumber(3).body("SAGE-SUGGESTION: off by one")
                .source(LineCommentSource.SAGE).status(LineCommentStatus.SUGGESTED).build());

        // The tutor's own published comment.
        data(post("/api/submissions/" + submissionId + "/line-comments"), tutor.token(),
                Map.of("lineNumber", 3, "body", "TUTOR-COMMENT: check the loop bound"), 201);

        // The tutor sees everything.
        assertThat(getJson("/api/submissions/" + submissionId + "/line-comments", tutor.token(), 200)
                .get("data")).hasSize(2);

        // Before release the student sees none of it.
        String ownSubmission = "/api/student/assignments/" + assignmentId + "/submission";
        String ownComments = "/api/student/submissions/" + submissionId + "/line-comments";
        JsonNode before = getJson(ownSubmission, student.token(), 200);
        assertThat(before.toString()).doesNotContain("SAGE-DRAFT", "SAGE-SUGGESTION", "TUTOR-COMMENT");
        assertThat(before.get("data").get("grade").isNull()).isTrue();
        assertThat(getJson(ownComments, student.token(), 200).get("data")).isEmpty();

        data(patch("/api/submissions/" + submissionId + "/release"), tutor.token(),
                Map.of("tutorFeedback", "Nice work overall.", "grade", "A-"), 200);

        // After release: the tutor's feedback, grade and published comment, never the suggestion.
        JsonNode after = getJson(ownSubmission, student.token(), 200).get("data");
        assertThat(after.get("grade").asText()).isEqualTo("A-");
        assertThat(after.get("tutorFeedback").asText()).isEqualTo("Nice work overall.");
        JsonNode comments = getJson(ownComments, student.token(), 200);
        assertThat(comments.get("data")).hasSize(1);
        assertThat(comments.toString()).contains("TUTOR-COMMENT").doesNotContain("SAGE-SUGGESTION");
    }

    @Test
    void studentsCannotSeeEachOthersWorkOrSubmitOutsideTheirCourses() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        Account classmate = register("STUDENT");
        Account outsider = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        join(classmate, course);
        UUID assignmentId = createAssignment(tutor, course);
        UUID submissionId = submit(student, assignmentId, CODE);

        getJson("/api/student/submissions/" + submissionId + "/line-comments", classmate.token(), 404);
        call(post("/api/assignments/" + assignmentId + "/submit"), outsider.token(), Map.of("content", CODE), 403);

        // Another tutor can't read it either (404, not 403: existence isn't confirmed).
        Account otherTutor = register("TUTOR");
        getJson("/api/submissions/" + submissionId, otherTutor.token(), 404);
    }
}
