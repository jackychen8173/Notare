package com.notare.discussion;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Checks the raw JSON, not just the name field, so an id or name leaking anywhere fails the test. */
class DiscussionPrivacyTest extends IntegrationTest {

    @Test
    void anonymousPostsHideTheAuthorFromClassmatesButNotTheTutor() throws Exception {
        Account tutor = register("TUTOR");
        Account author = register("STUDENT");
        Account classmate = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(author, course);
        join(classmate, course);
        String courseId = course.get("id").asText();

        JsonNode thread = data(post("/api/student/courses/" + courseId + "/discussions"), author.token(),
                Map.of("title", "Confused about recursion", "body", "Base cases?", "visibility", "PUBLIC",
                        "anonymous", true), 201);
        String threadId = thread.get("id").asText();
        data(post("/api/student/discussions/" + threadId + "/posts"), author.token(),
                Map.of("body", "Also, stack overflow errors", "anonymous", true), 201);

        String classmateList = getJson("/api/student/courses/" + courseId + "/discussions", classmate.token(), 200).toString();
        String classmateView = getJson("/api/student/discussions/" + threadId, classmate.token(), 200).toString();
        for (String view : new String[]{classmateList, classmateView}) {
            assertThat(view).contains("Confused about recursion")
                    .doesNotContain(author.name()).doesNotContain(author.id().toString());
        }

        JsonNode tutorView = getJson("/api/discussions/" + threadId, tutor.token(), 200).get("data");
        assertThat(tutorView.get("author").get("name").asText()).isEqualTo(author.name());
        assertThat(tutorView.get("author").get("anonymous").asBoolean()).isTrue();
    }

    @Test
    void privateThreadsAreInvisibleToClassmatesAndAnonymizedWhenMadePublic() throws Exception {
        Account tutor = register("TUTOR");
        Account author = register("STUDENT");
        Account classmate = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(author, course);
        join(classmate, course);
        String courseId = course.get("id").asText();

        JsonNode thread = data(post("/api/student/courses/" + courseId + "/discussions"), author.token(),
                Map.of("title", "Question about my grade", "body", "Can we talk?", "visibility", "PRIVATE"), 201);
        String threadId = thread.get("id").asText();

        assertThat(getJson("/api/student/courses/" + courseId + "/discussions", classmate.token(), 200).toString())
                .doesNotContain("Question about my grade");
        getJson("/api/student/discussions/" + threadId, classmate.token(), 404);
        assertThat(getJson("/api/discussions/" + threadId, tutor.token(), 200).toString()).contains(author.name());

        call(post("/api/discussions/" + threadId + "/make-public"), tutor.token(), null, 200);

        String classmateView = getJson("/api/student/discussions/" + threadId, classmate.token(), 200).toString();
        assertThat(classmateView).contains("Question about my grade")
                .doesNotContain(author.name()).doesNotContain(author.id().toString());
    }
}
