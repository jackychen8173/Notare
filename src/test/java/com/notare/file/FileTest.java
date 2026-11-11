package com.notare.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * File uploads: who can download what. Material files reach the tutor and enrolled students;
 * a student's files reach only them until they submit, then the tutor too. Nobody else, ever.
 */
class FileTest extends IntegrationTest {

    private static MockMultipartFile file(String name, String type, String text) {
        return new MockMultipartFile("file", name, type, text.getBytes(StandardCharsets.UTF_8));
    }

    /** Like call(), for multipart requests (a different builder type in Spring 7). */
    private JsonNode send(org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder request,
                          String token, int expectedStatus) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mvc.perform(request).andReturn();
        String text = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).as(text).isEqualTo(expectedStatus);
        return text.isBlank() ? json.nullNode() : json.readTree(text);
    }

    private JsonNode uploadMaterial(Account tutor, String courseId, MockMultipartFile file, int status) throws Exception {
        return send(multipart("/api/courses/" + courseId + "/materials/file").file(file).param("title", "Handout"),
                tutor.token(), status);
    }

    private JsonNode uploadDraft(Account student, UUID assignmentId, MockMultipartFile file, int status) throws Exception {
        return send(multipart("/api/student/assignments/" + assignmentId + "/files").file(file), student.token(), status);
    }

    private String link(Account account, String fileId, int status) throws Exception {
        JsonNode body = getJson("/api/files/" + fileId + "/link", account.token(), status);
        return status == 200 ? body.get("data").get("url").asText() : null;
    }

    /** Follows a download link with no credentials, like a browser opening it. */
    private MvcResult download(String url) throws Exception {
        return mvc.perform(get(java.net.URI.create(url))).andReturn();
    }

    @Test
    void materialFilesReachTheTutorAndEnrolledStudentsOnly() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        Account outsider = register("STUDENT");
        Account otherTutor = register("TUTOR");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();
        join(student, course);

        JsonNode material = uploadMaterial(tutor, courseId, file("../../Unit 1 notes.pdf", "application/pdf", "%PDF-1.4 notes"), 201)
                .get("data");
        assertThat(material.get("type").asText()).isEqualTo("FILE");
        assertThat(material.get("title").asText()).isEqualTo("Handout");
        String fileId = material.get("file").get("id").asText();
        // Only the last path segment of a name is kept.
        assertThat(material.get("file").get("fileName").asText()).isEqualTo("Unit 1 notes.pdf");

        JsonNode listed = getJson("/api/student/courses/" + courseId + "/materials", student.token(), 200).get("data");
        assertThat(listed.get(0).get("file").get("id").asText()).isEqualTo(fileId);

        MvcResult got = download(link(student, fileId, 200));
        assertThat(got.getResponse().getStatus()).isEqualTo(200);
        assertThat(got.getResponse().getContentAsString()).isEqualTo("%PDF-1.4 notes");
        // A PDF may show in the browser.
        assertThat(got.getResponse().getHeader("Content-Disposition")).startsWith("inline;");

        link(outsider, fileId, 404);
        link(otherTutor, fileId, 404);
        // A tampered link doesn't work.
        assertThat(download(link(tutor, fileId, 200).replace("signature=", "signature=0")).getResponse().getStatus()).isEqualTo(404);

        // File materials only come through the upload endpoint.
        call(post("/api/courses/" + courseId + "/materials"), tutor.token(), Map.of("title", "x", "type", "FILE"), 400);

        // Deleting the material deletes the file.
        String before = link(tutor, fileId, 200);
        call(delete("/api/materials/" + material.get("id").asText()), tutor.token(), null, 200);
        link(tutor, fileId, 404);
        assertThat(download(before).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void aStudentsFilesAreTheirsUntilTheySubmitThenTheTutorSeesThem() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        Account classmate = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        join(classmate, course);
        UUID assignment = createAssignment(tutor, course);

        String draftId = uploadDraft(student, assignment, file("Main.java", "text/x-java", "class Main {}"), 201)
                .get("data").get("id").asText();
        assertThat(getJson("/api/student/assignments/" + assignment + "/files", student.token(), 200).get("data")).hasSize(1);
        assertThat(getJson("/api/student/assignments/" + assignment + "/files", classmate.token(), 200).get("data")).isEmpty();
        link(student, draftId, 200);
        link(classmate, draftId, 404);
        link(tutor, draftId, 404); // not handed in yet

        // Files alone are enough to submit; the draft becomes part of the submission.
        String submissionId = data(post("/api/assignments/" + assignment + "/submit"), student.token(),
                Map.of("content", ""), 201).get("id").asText();
        assertThat(getJson("/api/student/assignments/" + assignment + "/files", student.token(), 200).get("data")).isEmpty();
        JsonNode files = getJson("/api/submissions/" + submissionId + "/files", tutor.token(), 200).get("data");
        assertThat(files).hasSize(1);
        assertThat(getJson("/api/submissions/" + submissionId + "/files", student.token(), 200).get("data")).hasSize(1);
        getJson("/api/submissions/" + submissionId + "/files", classmate.token(), 404);

        // Java source isn't a type browsers may render: it always downloads.
        MvcResult got = download(link(tutor, draftId, 200));
        assertThat(got.getResponse().getContentAsString()).isEqualTo("class Main {}");
        assertThat(got.getResponse().getHeader("Content-Disposition")).startsWith("attachment;");
        call(delete("/api/files/" + draftId), student.token(), null, 409);
    }

    @Test
    void draftsCanBeRemovedAndEmptySubmissionsAreRejected() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignment = createAssignment(tutor, course);

        String draftId = uploadDraft(student, assignment, file("notes.txt", "text/plain", "hi"), 201).get("data").get("id").asText();
        call(delete("/api/files/" + draftId), student.token(), null, 200);
        call(post("/api/assignments/" + assignment + "/submit"), student.token(), Map.of("content", "  "), 400);

        uploadDraft(student, assignment, file("empty.txt", "text/plain", ""), 400);
        // An HTML upload never renders on the storage domain.
        String html = uploadDraft(student, assignment, file("page.html", "text/html", "<script>alert(1)</script>"), 201)
                .get("data").get("id").asText();
        MvcResult got = download(link(student, html, 200));
        assertThat(got.getResponse().getHeader("Content-Disposition")).startsWith("attachment;");
        assertThat(got.getResponse().getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void unopenedAssignmentsAndDemoAccountsCantUpload() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        String later = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        String muchLater = LocalDateTime.now(ZoneId.of("America/Vancouver")).plusDays(9).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        UUID scheduled = UUID.fromString(data(post("/api/courses/" + course.get("id").asText() + "/assignments"), tutor.token(),
                Map.of("title", "Later", "dueAt", muchLater, "opensAt", later), 201).get("id").asText());
        uploadDraft(student, scheduled, file("Main.java", "text/x-java", "x"), 404);

        String demoToken = data(post("/api/auth/demo"), null, Map.of("role", "TUTOR"), 201).get("token").asText();
        String demoCourse = getJson("/api/courses", demoToken, 200).get("data").get(0).get("id").asText();
        send(multipart("/api/courses/" + demoCourse + "/materials/file").file(file("a.pdf", "application/pdf", "x")),
                demoToken, 403);
    }

    @Test
    void theUiCanAskWhetherUploadsAreOn() throws Exception {
        Account student = register("STUDENT");
        assertThat(getJson("/api/files/enabled", student.token(), 200).get("data").get("enabled").asBoolean()).isTrue();
    }

    @Test
    void fileNamesAreCleanedAndHeadersAreSafe() {
        assertThat(FileNames.clean("C:\\Users\\me\\Main.java")).isEqualTo("Main.java");
        assertThat(FileNames.clean("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileNames.clean("..")).isEqualTo("file");
        assertThat(FileNames.clean("a\"b\nc.txt")).isEqualTo("abc.txt");
        assertThat(FileNames.contentDisposition("résumé.pdf", false))
                .isEqualTo("attachment; filename=\"r_sum_.pdf\"; filename*=UTF-8''r%C3%A9sum%C3%A9.pdf");
        assertThat(FileNames.contentType("TEXT/HTML; charset=utf-8")).isEqualTo("text/html");
        assertThat(FileNames.inline("text/html")).isFalse();
        assertThat(FileNames.inline("image/png")).isTrue();
    }
}
