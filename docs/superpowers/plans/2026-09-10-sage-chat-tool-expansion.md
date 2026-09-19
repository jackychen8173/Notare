# Sage Chat Tool Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Sage chat 8 new read tools and 11 new confirm-gated write tools covering materials,
topics, grade categories, rubric scores, session notes, and course/assignment editing, closing the
gap between what Sage can do and what the app's UI already supports.

**Architecture:** No new mechanism. Every addition is more entries in the existing
`SageToolDefinitions.ALL_TOOLS` list, more `switch` branches in the existing
`SageToolExecutor.execute()`/`describeAction()`, and one new tool-schema builder overload for the
one genuinely new shape (`update_rubric_scores`'s array-of-objects parameter, confirmed against the
real `anthropic-java` 2.34.0 jar via `javap` during plan authoring — `JsonValue.from(Object)` walks
nested `Map`/`List` structures through Jackson's tree conversion, so a nested `Map.of(...)` passed
to the existing `putAdditionalProperty(String, JsonValue)` builder call nests correctly, not
guessed). No database, API, or frontend changes — `SageChatMessage.tsx` already renders whatever
description string the backend sends, regardless of tool name.

**Tech Stack:** Spring Boot 4.1 / Java 21 (backend, existing), `anthropic-java` 2.34.0 (existing
dependency). No frontend changes.

**Spec:** `docs/superpowers/specs/2026-09-10-sage-chat-tool-expansion-design.md`

## Global Constraints

- No automated test suite exists anywhere in this repo (`src/test/` is empty despite test starters
  being on the classpath — a pre-existing, documented gap this plan does not address). Every task
  substitutes a real compile check plus a real, temporary verification harness for TDD's red/green
  steps — see "Verification technique" below.
- Every new write tool's `describeAction` branch must show the tutor the **full, untruncated**
  value of any free-text field before they confirm it — no truncation, no omission. This is a
  binding constraint carried forward from the final whole-branch review of the original Sage chat
  feature, which found and fixed exactly this class of bug (`release_feedback` omitting
  `tutorFeedback`, `post_announcement` truncating `content` at 80 characters).
- Every tool delegates to an already-existing service method that does its own tutor-ownership
  check by email (the same 404-not-403 pattern used throughout this codebase) — never a new
  ownership-check mechanism, never trust anything from the model's `input` map without validating
  it through `uuidParam`/`dateTimeParam`/`intParam`-style helpers (add `bigDecimalParam` and
  `localDateParam` alongside them, following the exact same shape).
- JDK: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.7.6-hotspot"` before any
  `./mvnw.cmd` command, per `.claude/skills/run-locally/SKILL.md`.
- Local verification: Postgres container `notare-local-pg` (port 55432) must be running
  (`docker start notare-local-pg`); backend via `bash scripts/run-backend.sh`. The backend must be
  **restarted** after every backend code change in this plan before curl-testing it — it does not
  hot-reload. **This environment has no reachable `ANTHROPIC_API_KEY`** (a hard, already-documented
  permission-classifier block on pulling one from the linked Railway service) — the real
  model-driven chat loop (`POST /api/sage/chat`) cannot be exercised. See "Verification technique"
  below for how this plan verifies new tools without it.

## Verification technique: a temporary debug endpoint (no ANTHROPIC_API_KEY needed)

`SageToolExecutor.execute(toolName, input, tutor)` and `.describeAction(...)` have **zero
dependency on the Anthropic client** — only `SageChatService.runLoop` calls the Anthropic API. So
every new tool can be verified directly, bypassing the chat loop entirely, by temporarily adding a
debug-only controller that calls the executor's methods straight from curl:

```java
// TEMPORARY — delete this file before the task's final commit. Never ship it.
package com.notare.sage.chat;

import com.notare.common.ApiResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/debug/sage-tool")
@PreAuthorize("hasRole('TUTOR')")
public class DebugSageToolController {

    private final SageToolExecutor executor;
    private final UserRepository userRepository;

    public DebugSageToolController(SageToolExecutor executor, UserRepository userRepository) {
        this.executor = executor;
        this.userRepository = userRepository;
    }

    public record DebugRequest(String toolName, Map<String, Object> input, boolean describeOnly) {
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> run(@RequestBody DebugRequest request, Authentication authentication) {
        User tutor = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not found"));
        String result = request.describeOnly()
                ? executor.describeAction(request.toolName(), request.input(), tutor)
                : executor.execute(request.toolName(), request.input(), tutor);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

Save it as `src/main/java/com/notare/sage/chat/DebugSageToolController.java`, restart the backend,
curl it (examples in each task below), confirm the results, then **delete the file** and restart
the backend again before that task's final commit. Never commit this file — it bypasses the
confirm/decline gate entirely and exists only so this plan can verify without a live model loop.

---

### Task 1: Tool schemas for the 8 new read tools and 11 new write tools

**Files:**
- Modify: `src/main/java/com/notare/sage/chat/SageToolDefinitions.java`

**Interfaces:**
- Consumes: nothing new (pure data, no service/repository calls).
- Produces: 19 new `Tool` entries in `ALL_TOOLS` (growing it from 14 to 33), plus a new private
  static `toolRaw(String name, String description, Map<String, Object> properties, List<String>
  required)` helper alongside the existing `tool(...)` helper, for schemas the flat `Param` record
  can't express. Task 2 and Task 3 consume `ALL_TOOLS` by name (`execute`/`describeAction` switch
  on the tool-name strings defined here) — every tool name below must match exactly what those
  tasks switch on.

- [ ] **Step 1: Add the `toolRaw` helper**

In `SageToolDefinitions.java`, immediately after the existing `tool(...)` method (right before the
`ALL_TOOLS` field), add:

```java
    private static Tool toolRaw(String name, String description, Map<String, Object> properties, List<String> required) {
        Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
        properties.forEach((key, schema) -> propsBuilder.putAdditionalProperty(key, JsonValue.from(schema)));
        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder().properties(propsBuilder.build());
        required.forEach(schemaBuilder::addRequired);
        return Tool.builder().name(name).description(description).inputSchema(schemaBuilder.build()).build();
    }
```

This mirrors `tool(...)` exactly, except each property's full JSON-Schema object is passed in
directly (as a `Map<String, Object>`, which can itself contain nested `Map`/`List` values) instead
of being built from the flat `Param(name, type, description)` record. `JsonValue.from(Object)`
(same call the existing `tool(...)` already uses per-property) walks nested collections via
Jackson's tree conversion, so a `Map` containing another `Map` — needed for `scores`' nested
`items` schema below — serializes as a real nested JSON object, not a stringified blob.

- [ ] **Step 2: Add the 8 new read tools to `ALL_TOOLS`**

In `SageToolDefinitions.java`, inside the `ALL_TOOLS` list, immediately after the
`get_student_progress` entry and before `release_feedback`, add:

```java
            tool("get_session", "Get a single tutoring session's detail by ID.",
                    List.of(new Param("sessionId", "string", "The session's UUID")),
                    List.of("sessionId")),
            tool("get_session_notes", "Get the raw and AI-formatted notes for a session, if any exist.",
                    List.of(new Param("sessionId", "string", "The session's UUID")),
                    List.of("sessionId")),
            tool("list_materials", "List the materials (readings, links, resources) attached to a course.",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("list_topics", "List the topics defined in a course, used to organize its materials and assignments.",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("get_rubric", "Get an assignment's grading rubric, including each criterion's name and points possible.",
                    List.of(new Param("assignmentId", "string", "The assignment's UUID")),
                    List.of("assignmentId")),
            tool("list_grade_categories", "List a course's grade categories and their weight percentages.",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("list_enrolled_students", "List only the students enrolled in a specific course (unlike list_students, which lists all of the tutor's students across every course).",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("list_pending_reviews", "Get the count of submissions awaiting the tutor's review.",
                    List.of(), List.of()),
```

- [ ] **Step 3: Add the 11 new write tools to `ALL_TOOLS`**

Immediately after `post_announcement`'s entry (now the last entry before the closing `);`), replace
the closing:

```java
                    List.of("courseId", "content"))
    );
```

with:

```java
                    List.of("courseId", "content")),
            tool("update_course",
                    "Update a course's name, subject, and description. This replaces all three "
                            + "fields, so look up the course's current values first and pass through "
                            + "any field you are not changing. This is a write action requiring tutor "
                            + "confirmation before it takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("name", "string", "The course's new name"),
                            new Param("subject", "string", "The course's new subject"),
                            new Param("description", "string", "Optional new description")),
                    List.of("courseId", "name", "subject")),
            tool("create_assignment",
                    "Create a new assignment in a course. This is a write action requiring tutor "
                            + "confirmation before it takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("title", "string", "The assignment's title"),
                            new Param("description", "string", "Optional assignment description"),
                            new Param("dueDate", "string", "ISO-8601 date, e.g. 2026-09-20"),
                            new Param("topicId", "string", "Optional topic UUID to associate this assignment with"),
                            new Param("gradeCategoryId", "string", "Optional grade category UUID this assignment counts toward")),
                    List.of("courseId", "title", "dueDate")),
            tool("create_material",
                    "Add a material (reading, link, or resource) to a course. This is a write action "
                            + "requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("title", "string", "The material's title"),
                            new Param("description", "string", "Optional description"),
                            new Param("url", "string", "Optional URL"),
                            new Param("topicId", "string", "Optional topic UUID to file this material under")),
                    List.of("courseId", "title")),
            tool("create_topic",
                    "Add a topic to a course, used to organize its materials and assignments. This is "
                            + "a write action requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("name", "string", "The topic's name")),
                    List.of("courseId", "name")),
            tool("rename_topic",
                    "Rename an existing topic. This is a write action requiring tutor confirmation "
                            + "before it takes effect.",
                    List.of(
                            new Param("topicId", "string", "The topic's UUID"),
                            new Param("name", "string", "The topic's new name")),
                    List.of("topicId", "name")),
            tool("create_grade_category",
                    "Add a grade category (e.g. \"Homework\", \"Exams\") to a course, with a weight "
                            + "percentage. This is a write action requiring tutor confirmation before it "
                            + "takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("name", "string", "The grade category's name"),
                            new Param("weightPercent", "number", "Weight percentage, 0-100")),
                    List.of("courseId", "name", "weightPercent")),
            tool("update_grade_category",
                    "Update a grade category's name and weight percentage. This replaces both fields, "
                            + "so look up its current values first and pass through any field you are "
                            + "not changing. This is a write action requiring tutor confirmation before "
                            + "it takes effect.",
                    List.of(
                            new Param("categoryId", "string", "The grade category's UUID"),
                            new Param("name", "string", "The grade category's new name"),
                            new Param("weightPercent", "number", "New weight percentage, 0-100")),
                    List.of("categoryId", "name", "weightPercent")),
            tool("save_session_notes",
                    "Write or overwrite the raw notes for a tutoring session. This is a write action "
                            + "requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("sessionId", "string", "The session's UUID"),
                            new Param("rawNotes", "string", "The raw session notes text")),
                    List.of("sessionId", "rawNotes")),
            toolRaw("update_rubric_scores",
                    "Set a submission's rubric criterion scores. Resolve criterion IDs via get_rubric "
                            + "or get_submission first - never guess an ID. This is a write action "
                            + "requiring tutor confirmation before it takes effect.",
                    Map.of(
                            "submissionId", Map.of("type", "string", "description", "The submission's UUID"),
                            "scores", Map.of(
                                    "type", "array",
                                    "description", "The rubric criterion scores to set",
                                    "items", Map.of(
                                            "type", "object",
                                            "properties", Map.of(
                                                    "criterionId", Map.of("type", "string", "description", "The rubric criterion's UUID"),
                                                    "pointsAwarded", Map.of("type", "number", "description", "Points to award for this criterion")
                                            ),
                                            "required", List.of("criterionId", "pointsAwarded")
                                    )
                            )
                    ),
                    List.of("submissionId", "scores")),
            tool("draft_session_notes",
                    "Ask Sage to draft AI-formatted notes from a session's existing raw notes. Raw "
                            + "notes must already be saved for this session. This is a write action "
                            + "requiring tutor confirmation before it takes effect.",
                    List.of(new Param("sessionId", "string", "The session's UUID")),
                    List.of("sessionId")),
            tool("review_submission",
                    "Ask Sage to generate AI feedback for a submission. The feedback is stored but NOT "
                            + "shown to the student until the tutor separately calls release_feedback. "
                            + "This is a write action requiring tutor confirmation before it takes effect.",
                    List.of(new Param("submissionId", "string", "The submission's UUID")),
                    List.of("submissionId"))
    );
```

- [ ] **Step 4: Compile**

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.7.6-hotspot"
./mvnw.cmd -q compile
```

Expected: no output, exit code 0.

- [ ] **Step 5: Verify the schema shapes for real**

`SageToolExecutor` doesn't reference these new tool names yet (Tasks 2-3), so nothing calls them —
verify the schemas themselves compiled correctly by printing them. Add a throwaway `main` method
temporarily to `SageToolDefinitions.java` (delete it before committing):

```java
    public static void main(String[] args) {
        System.out.println("Total tools: " + ALL_TOOLS.size());
        ALL_TOOLS.stream()
                .filter(t -> t.name().equals("update_rubric_scores"))
                .findFirst()
                .ifPresent(t -> System.out.println(t.inputSchema()));
    }
```

Run: `./mvnw.cmd -q compile exec:java -Dexec.mainClass=com.notare.sage.chat.SageToolDefinitions` — if
the `exec-maven-plugin` isn't configured in `pom.xml`, instead run it via
`java -cp target/classes:$(./mvnw.cmd -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null) com.notare.sage.chat.SageToolDefinitions`
(Windows Git Bash: use `;` instead of `:` as the classpath separator).

Expected: `Total tools: 33`, and the printed `update_rubric_scores` input schema shows `scores` as a
nested structure with `items`, `properties` containing `criterionId`/`pointsAwarded`, and `required`
actually nested — not flattened into a string. If it prints flattened or throws, STOP and report
back rather than guessing a fix; this is the one genuinely novel schema shape in this plan.

Delete the `main` method once confirmed.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/notare/sage/chat/SageToolDefinitions.java
git commit -m "Add tool schemas for 8 new Sage read tools and 11 new write tools"
```

---

### Task 2: 8 new read tools in `SageToolExecutor`

**Files:**
- Modify: `src/main/java/com/notare/sage/chat/SageToolExecutor.java`

**Interfaces:**
- Consumes: the 8 read-tool names from Task 1 (`get_session`, `get_session_notes`,
  `list_materials`, `list_topics`, `get_rubric`, `list_grade_categories`, `list_enrolled_students`,
  `list_pending_reviews`); existing methods `SessionService.getSession(UUID, String)`,
  `SessionService.getSessionNotes(UUID, String)`, `SageService.pendingReviewsCount(String)` (all
  already injected as fields); newly-injected `TopicService.listTopics(UUID, String)`,
  `MaterialService.listMaterials(UUID, String)`, `GradeCategoryService.listCategories(UUID,
  String)`, `RubricService.getRubric(UUID, String)`, `CourseService.listEnrolledStudents(UUID,
  String)`.
- Produces: 5 new constructor-injected fields (`topicService`, `materialService`,
  `gradeCategoryService`, `rubricService`, `courseService`) that Task 3 also uses.

- [ ] **Step 1: Add imports and constructor dependencies**

In `SageToolExecutor.java`, add these imports alongside the existing ones:

```java
import com.notare.course.CourseService;
import com.notare.gradecategory.GradeCategoryService;
import com.notare.gradecategory.dto.GradeCategoryResponse;
import com.notare.material.MaterialService;
import com.notare.material.dto.MaterialResponse;
import com.notare.rubric.RubricService;
import com.notare.rubric.dto.RubricResponse;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.topic.TopicService;
import com.notare.topic.dto.TopicResponse;
```

Add 5 new fields right after the existing `objectMapper` field:

```java
    private final TopicService topicService;
    private final MaterialService materialService;
    private final GradeCategoryService gradeCategoryService;
    private final RubricService rubricService;
    private final CourseService courseService;
```

Extend the constructor's parameter list (add these 5 after the existing `ObjectMapper objectMapper`
parameter) and assign them in the body, following the exact same pattern as every existing
parameter:

```java
    public SageToolExecutor(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SubmissionService submissionService,
            SessionService sessionService,
            AnnouncementService announcementService,
            SageService sageService,
            ObjectMapper objectMapper,
            TopicService topicService,
            MaterialService materialService,
            GradeCategoryService gradeCategoryService,
            RubricService rubricService,
            CourseService courseService
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.sessionService = sessionService;
        this.announcementService = announcementService;
        this.sageService = sageService;
        this.objectMapper = objectMapper;
        this.topicService = topicService;
        this.materialService = materialService;
        this.gradeCategoryService = gradeCategoryService;
        this.rubricService = rubricService;
        this.courseService = courseService;
    }
```

- [ ] **Step 2: Add the 8 new cases to `execute()`'s switch**

In `execute()`, immediately after the `case "get_student_progress" -> getStudentProgress(input, tutor);`
line and before `case "release_feedback" -> releaseFeedback(input, tutor);`, add:

```java
            case "get_session" -> getSession(input, tutor);
            case "get_session_notes" -> getSessionNotes(input, tutor);
            case "list_materials" -> listMaterials(input, tutor);
            case "list_topics" -> listTopics(input, tutor);
            case "get_rubric" -> getRubric(input, tutor);
            case "list_grade_categories" -> listGradeCategories(input, tutor);
            case "list_enrolled_students" -> listEnrolledStudents(input, tutor);
            case "list_pending_reviews" -> listPendingReviews(input, tutor);
```

- [ ] **Step 3: Add the 8 new private read-tool methods**

Immediately after the existing `getStudentProgress` method (still inside the `// ---- read tools
----` section, before `// ---- write tools ----`), add:

```java
    private SessionResponse getSession(Map<String, Object> input, User tutor) {
        return sessionService.getSession(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private SessionNoteResponse getSessionNotes(Map<String, Object> input, User tutor) {
        return sessionService.getSessionNotes(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private List<MaterialResponse> listMaterials(Map<String, Object> input, User tutor) {
        return materialService.listMaterials(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private List<TopicResponse> listTopics(Map<String, Object> input, User tutor) {
        return topicService.listTopics(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private RubricResponse getRubric(Map<String, Object> input, User tutor) {
        return rubricService.getRubric(uuidParam(input, "assignmentId"), tutor.getEmail());
    }

    private List<GradeCategoryResponse> listGradeCategories(Map<String, Object> input, User tutor) {
        return gradeCategoryService.listCategories(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private List<StudentResponse> listEnrolledStudents(Map<String, Object> input, User tutor) {
        return courseService.listEnrolledStudents(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private PendingReviewsResponse listPendingReviews(Map<String, Object> input, User tutor) {
        return sageService.pendingReviewsCount(tutor.getEmail());
    }
```

(`listPendingReviews` takes an unused `input` parameter purely to match every other case's uniform
`(input, tutor) ->` shape in the switch — `list_pending_reviews` has no tool parameters.)

- [ ] **Step 4: Compile**

```bash
./mvnw.cmd -q compile
```

Expected: no output, exit code 0.

- [ ] **Step 5: Verify against real data via the temporary debug endpoint**

Add `DebugSageToolController.java` from "Verification technique" above. Restart the backend:

```bash
docker start notare-local-pg
bash scripts/run-backend.sh &
until curl -s -o /dev/null http://localhost:8080/v3/api-docs; do sleep 2; done
```

Log in as an existing local tutor fixture (reuse whatever tutor/course/student exists locally from
prior sessions — e.g. `localtest+run@example.com` / `password123`, per this repo's established
local fixture data) and grab a course ID and, if one exists, a session ID:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"localtest+run@example.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s http://localhost:8080/api/courses -H "Authorization: Bearer $TOKEN"
# pick a real courseId from the response for the calls below
```

For each new read tool, call it through the debug endpoint and confirm the JSON result matches what
the equivalent existing REST GET returns for the same ID (e.g. `list_materials` for a course should
match `GET /api/courses/{courseId}/materials`):

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"list_materials","input":{"courseId":"<real courseId>"},"describeOnly":false}'
# Expect 200 and a JSON array matching GET /api/courses/{courseId}/materials

curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"list_topics","input":{"courseId":"<real courseId>"},"describeOnly":false}'

curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"list_grade_categories","input":{"courseId":"<real courseId>"},"describeOnly":false}'

curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"list_enrolled_students","input":{"courseId":"<real courseId>"},"describeOnly":false}'

curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"list_pending_reviews","input":{},"describeOnly":false}'
# Expect 200 and {"count":<N>}
```

If a real session or assignment (for `get_session`/`get_session_notes`/`get_rubric`) doesn't exist
in the local fixture data yet, create one first via the existing (already-shipped) REST endpoints
(`POST /api/sessions`, `POST /api/assignments/{id}/rubric`, etc.), then verify `get_session`,
`get_session_notes`, and `get_rubric` the same way — 200, and a JSON body matching the equivalent
existing `GET` endpoint. Also verify one ownership-check case: call any new read tool with an ID
belonging to a different tutor's data (or a random UUID) and confirm it returns the tool's `is_error`
shape (via the debug endpoint's `execute` path, a `ResponseStatusException(404)` surfaces as an
error — confirm it does NOT leak data).

Delete `DebugSageToolController.java`, restart the backend once more to confirm it's gone (a 404 on
`/api/debug/sage-tool`), and confirm the app still starts cleanly without it.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/notare/sage/chat/SageToolExecutor.java
git commit -m "Add 8 new Sage read tools: sessions, materials, topics, rubrics, grade categories, pending reviews"
```

---

### Task 3: 11 new write tools in `SageToolExecutor`

**Files:**
- Modify: `src/main/java/com/notare/sage/chat/SageToolExecutor.java`

**Interfaces:**
- Consumes: the 11 write-tool names from Task 1; `topicService`, `materialService`,
  `gradeCategoryService`, `courseService` (Task 2's fields, reused here for their mutating methods);
  existing `SessionService.saveSessionNotes`, `SubmissionService.updateRubricScores`,
  `SageService.draftSessionNotes`, `SageService.reviewSubmission`; newly-injected
  `AssignmentService.createAssignment(UUID, CreateAssignmentRequest, String)`,
  `TopicRepository.findById`, `GradeCategoryRepository.findById`,
  `RubricCriterionRepository.findById`.
- Produces: `WRITE_TOOL_NAMES` grows from 4 to 15 entries — Task 4 doesn't consume this directly,
  but the final whole-branch review will check every entry here has a matching `describeAction`
  branch, same as the original feature's review found and required.

- [ ] **Step 1: Add imports and 4 more constructor dependencies**

Add these imports:

```java
import com.notare.assignment.AssignmentService;
import com.notare.assignment.dto.CreateAssignmentRequest;
import com.notare.course.dto.UpdateCourseRequest;
import com.notare.gradecategory.GradeCategory;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.gradecategory.dto.CreateGradeCategoryRequest;
import com.notare.gradecategory.dto.UpdateGradeCategoryRequest;
import com.notare.material.dto.CreateMaterialRequest;
import com.notare.rubric.RubricCriterion;
import com.notare.rubric.RubricCriterionRepository;
import com.notare.session.dto.SaveSessionNotesRequest;
import com.notare.submission.dto.UpdateRubricScoresRequest;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.topic.dto.CreateTopicRequest;
import com.notare.topic.dto.RenameTopicRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
```

Add 4 new fields after the fields Task 2 added:

```java
    private final AssignmentService assignmentService;
    private final TopicRepository topicRepository;
    private final GradeCategoryRepository gradeCategoryRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
```

Extend the constructor again (add these 4 params after Task 2's 5, assign in the body the same
way):

```java
    public SageToolExecutor(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SubmissionService submissionService,
            SessionService sessionService,
            AnnouncementService announcementService,
            SageService sageService,
            ObjectMapper objectMapper,
            TopicService topicService,
            MaterialService materialService,
            GradeCategoryService gradeCategoryService,
            RubricService rubricService,
            CourseService courseService,
            AssignmentService assignmentService,
            TopicRepository topicRepository,
            GradeCategoryRepository gradeCategoryRepository,
            RubricCriterionRepository rubricCriterionRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.sessionService = sessionService;
        this.announcementService = announcementService;
        this.sageService = sageService;
        this.objectMapper = objectMapper;
        this.topicService = topicService;
        this.materialService = materialService;
        this.gradeCategoryService = gradeCategoryService;
        this.rubricService = rubricService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.topicRepository = topicRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
    }
```

- [ ] **Step 2: Grow `WRITE_TOOL_NAMES`**

Replace:

```java
    public static final Set<String> WRITE_TOOL_NAMES =
            Set.of("release_feedback", "schedule_session", "complete_session", "post_announcement");
```

with:

```java
    public static final Set<String> WRITE_TOOL_NAMES = Set.of(
            "release_feedback", "schedule_session", "complete_session", "post_announcement",
            "update_course", "create_assignment", "create_material", "create_topic", "rename_topic",
            "create_grade_category", "update_grade_category", "save_session_notes",
            "update_rubric_scores", "draft_session_notes", "review_submission"
    );
```

- [ ] **Step 3: Add the 11 new cases to `execute()`'s switch**

Immediately after `case "post_announcement" -> postAnnouncement(input, tutor);` and before the
`default ->` line, add:

```java
            case "update_course" -> updateCourse(input, tutor);
            case "create_assignment" -> createAssignment(input, tutor);
            case "create_material" -> createMaterial(input, tutor);
            case "create_topic" -> createTopic(input, tutor);
            case "rename_topic" -> renameTopic(input, tutor);
            case "create_grade_category" -> createGradeCategory(input, tutor);
            case "update_grade_category" -> updateGradeCategory(input, tutor);
            case "save_session_notes" -> saveSessionNotes(input, tutor);
            case "update_rubric_scores" -> updateRubricScores(input, tutor);
            case "draft_session_notes" -> draftSessionNotes(input, tutor);
            case "review_submission" -> reviewSubmission(input, tutor);
```

- [ ] **Step 4: Add the 11 new cases to `describeAction()`'s switch**

Immediately after the existing `case "post_announcement" -> { ... }` block and before the `default
->` line, add:

```java
            case "update_course" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String descriptionClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\""
                        : "";
                yield "Update course \"" + course.getName() + "\" to name \"" + input.get("name")
                        + "\", subject \"" + input.get("subject") + "\"." + descriptionClause;
            }
            case "create_assignment" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String descClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\"" : "";
                yield "Create assignment \"" + input.get("title") + "\" in " + course.getName()
                        + ", due " + input.get("dueDate") + "." + descClause;
            }
            case "create_material" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String urlClause = input.get("url") != null ? " (" + input.get("url") + ")" : "";
                String descClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\"" : "";
                yield "Add material \"" + input.get("title") + "\" to " + course.getName() + "."
                        + urlClause + descClause;
            }
            case "create_topic" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                yield "Add topic \"" + input.get("name") + "\" to " + course.getName() + ".";
            }
            case "rename_topic" -> {
                Topic topic = requireOwnedTopic(uuidParam(input, "topicId"), tutor);
                yield "Rename topic \"" + topic.getName() + "\" to \"" + input.get("name")
                        + "\" in " + topic.getCourse().getName() + ".";
            }
            case "create_grade_category" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                yield "Add grade category \"" + input.get("name") + "\" (" + input.get("weightPercent")
                        + "%) to " + course.getName() + ".";
            }
            case "update_grade_category" -> {
                GradeCategory category = requireOwnedGradeCategory(uuidParam(input, "categoryId"), tutor);
                yield "Update grade category \"" + category.getName() + "\" to \"" + input.get("name")
                        + "\" (" + input.get("weightPercent") + "%).";
            }
            case "save_session_notes" -> {
                Session session = requireOwnedSession(uuidParam(input, "sessionId"), tutor);
                yield "Save session notes for the session with " + session.getStudent().getName()
                        + " on " + session.getDate() + ": \"" + input.get("rawNotes") + "\"";
            }
            case "update_rubric_scores" -> {
                Submission submission = requireOwnedSubmission(uuidParam(input, "submissionId"), tutor);
                yield "Set rubric scores for " + submission.getStudent().getName()
                        + "'s submission on \"" + submission.getAssignment().getTitle() + "\": "
                        + describeScores(input);
            }
            case "draft_session_notes" -> {
                Session session = requireOwnedSession(uuidParam(input, "sessionId"), tutor);
                yield "Ask Sage to draft formatted notes from the raw notes for the session with "
                        + session.getStudent().getName() + " on " + session.getDate() + ".";
            }
            case "review_submission" -> {
                Submission submission = requireOwnedSubmission(uuidParam(input, "submissionId"), tutor);
                yield "Ask Sage to generate AI feedback for " + submission.getStudent().getName()
                        + "'s submission on \"" + submission.getAssignment().getTitle()
                        + "\" (not visible to the student until you release it).";
            }
```

- [ ] **Step 5: Add the 11 new private execute methods, `describeScores`, and 2 new helpers**

Immediately after the existing `postAnnouncement` method (still inside `// ---- write tools ----`),
add:

```java
    private CourseResponse updateCourse(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String name = String.valueOf(input.get("name"));
        String subject = String.valueOf(input.get("subject"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        return courseService.updateCourse(courseId, new UpdateCourseRequest(name, subject, description), tutor.getEmail());
    }

    private AssignmentResponse createAssignment(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String title = String.valueOf(input.get("title"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        LocalDate dueDate = localDateParam(input, "dueDate");
        UUID topicId = input.get("topicId") != null ? uuidParam(input, "topicId") : null;
        UUID gradeCategoryId = input.get("gradeCategoryId") != null ? uuidParam(input, "gradeCategoryId") : null;
        return assignmentService.createAssignment(courseId,
                new CreateAssignmentRequest(title, description, dueDate, topicId, gradeCategoryId), tutor.getEmail());
    }

    private MaterialResponse createMaterial(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String title = String.valueOf(input.get("title"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        String url = input.get("url") != null ? String.valueOf(input.get("url")) : null;
        UUID topicId = input.get("topicId") != null ? uuidParam(input, "topicId") : null;
        return materialService.createMaterial(courseId, new CreateMaterialRequest(title, description, url, topicId), tutor.getEmail());
    }

    private TopicResponse createTopic(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        return topicService.createTopic(courseId, new CreateTopicRequest(String.valueOf(input.get("name"))), tutor.getEmail());
    }

    private TopicResponse renameTopic(Map<String, Object> input, User tutor) {
        UUID topicId = uuidParam(input, "topicId");
        return topicService.renameTopic(topicId, new RenameTopicRequest(String.valueOf(input.get("name"))), tutor.getEmail());
    }

    private GradeCategoryResponse createGradeCategory(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        BigDecimal weightPercent = bigDecimalParam(input, "weightPercent");
        return gradeCategoryService.createCategory(courseId,
                new CreateGradeCategoryRequest(String.valueOf(input.get("name")), weightPercent), tutor.getEmail());
    }

    private GradeCategoryResponse updateGradeCategory(Map<String, Object> input, User tutor) {
        UUID categoryId = uuidParam(input, "categoryId");
        BigDecimal weightPercent = bigDecimalParam(input, "weightPercent");
        return gradeCategoryService.updateCategory(categoryId,
                new UpdateGradeCategoryRequest(String.valueOf(input.get("name")), weightPercent), tutor.getEmail());
    }

    private SessionNoteResponse saveSessionNotes(Map<String, Object> input, User tutor) {
        UUID sessionId = uuidParam(input, "sessionId");
        return sessionService.saveSessionNotes(sessionId,
                new SaveSessionNotesRequest(String.valueOf(input.get("rawNotes"))), tutor.getEmail());
    }

    @SuppressWarnings("unchecked")
    private SubmissionResponse updateRubricScores(Map<String, Object> input, User tutor) {
        UUID submissionId = uuidParam(input, "submissionId");
        List<Map<String, Object>> rawScores = (List<Map<String, Object>>) input.get("scores");
        List<UpdateRubricScoresRequest.ScoreInput> scores = rawScores.stream()
                .map(s -> new UpdateRubricScoresRequest.ScoreInput(
                        UUID.fromString(String.valueOf(s.get("criterionId"))),
                        bigDecimalParam(s, "pointsAwarded")))
                .toList();
        return submissionService.updateRubricScores(submissionId, new UpdateRubricScoresRequest(scores), tutor.getEmail());
    }

    private SessionNoteResponse draftSessionNotes(Map<String, Object> input, User tutor) {
        return sageService.draftSessionNotes(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private SubmissionResponse reviewSubmission(Map<String, Object> input, User tutor) {
        return sageService.reviewSubmission(uuidParam(input, "submissionId"), tutor.getEmail());
    }

    @SuppressWarnings("unchecked")
    private String describeScores(Map<String, Object> input) {
        List<Map<String, Object>> scores = (List<Map<String, Object>>) input.get("scores");
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> score : scores) {
            UUID criterionId = UUID.fromString(String.valueOf(score.get("criterionId")));
            RubricCriterion criterion = rubricCriterionRepository.findById(criterionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid criterionId: " + criterionId));
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(criterion.getName()).append(": ").append(score.get("pointsAwarded"))
                    .append("/").append(criterion.getPointsPossible());
        }
        return sb.toString();
    }
```

Add `requireOwnedTopic` and `requireOwnedGradeCategory` alongside the existing
`requireOwnedCourse`/`requireOwnedAssignment`/`requireOwnedSubmission`/`requireOwnedSession`
helpers (same 404-not-403 pattern):

```java
    private Topic requireOwnedTopic(UUID topicId, User tutor) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!topic.getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
        }
        return topic;
    }

    private GradeCategory requireOwnedGradeCategory(UUID categoryId, User tutor) {
        GradeCategory category = gradeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found"));
        if (!category.getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found");
        }
        return category;
    }
```

Add `bigDecimalParam` and `localDateParam` alongside the existing `uuidParam`/`dateTimeParam`/
`intParam` helpers, following their exact shape:

```java
    private BigDecimal bigDecimalParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid number for " + key + ": " + value);
        }
    }

    private LocalDate localDateParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date for " + key + ": " + value);
        }
    }
```

- [ ] **Step 6: Compile**

```bash
./mvnw.cmd -q compile
```

Expected: no output, exit code 0.

- [ ] **Step 7: Verify against real data via the temporary debug endpoint**

Re-add `DebugSageToolController.java` (Task 2 deleted it). Restart the backend the same way as
Task 2's Step 5.

For each new write tool: call `describeOnly:true` first and confirm the description string
contains the **full** text of every free-text field you passed (per this plan's Global Constraints
— no truncation), then call `describeOnly:false` and confirm the underlying row actually changed
via the corresponding existing REST GET:

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"create_topic","input":{"courseId":"<real courseId>","name":"Fractions"},"describeOnly":true}'
# Expect the description to literally contain "Fractions"

curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"create_topic","input":{"courseId":"<real courseId>","name":"Fractions"},"describeOnly":false}'
curl -s http://localhost:8080/api/courses/<real courseId>/topics -H "Authorization: Bearer $TOKEN"
# Expect the new topic to actually be present
```

Repeat this pattern for `create_material`, `create_grade_category`, `create_assignment`,
`save_session_notes` (verify via `GET /api/sessions/{id}/notes`), `update_course` (verify via `GET
/api/courses/{id}`), `rename_topic`/`update_grade_category` (create one first via their `create_*`
tool, then rename/update it, verify via the corresponding list endpoint), `draft_session_notes` (a
raw note must already exist — `save_session_notes` first — then verify `formattedNotes` is
populated via `GET /api/sessions/{id}/notes`), and `review_submission` (needs a real submission —
if none exists locally, create one via the existing `POST /api/assignments/{id}/submit` flow as a
student first — then verify `sageFeedback` is populated via `GET /api/submissions/{id}`, and
confirm it is NOT visible via the student-facing submission endpoint until `release_feedback` is
also called, per the existing release-gating rule).

For `update_rubric_scores`: an assignment needs a real rubric with real criterion IDs first (create
one via the existing `POST /api/assignments/{id}/rubric` if none exists locally), then call
`get_rubric` (from Task 2) to get real `criterionId`s, then:

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:8080/api/debug/sage-tool \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toolName":"update_rubric_scores","input":{"submissionId":"<real submissionId>","scores":[{"criterionId":"<real criterionId>","pointsAwarded":8}]},"describeOnly":true}'
# Expect the description to name the real criterion by NAME (not just its ID) and show "8/<pointsPossible>"
```

confirm the description names the criterion, then `describeOnly:false` and verify via `GET
/api/submissions/{id}` that the score actually landed.

Delete `DebugSageToolController.java`, restart the backend once more to confirm it's gone.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/notare/sage/chat/SageToolExecutor.java
git commit -m "Add 11 new Sage write tools: course/assignment/material/topic/grade-category editing, rubric scoring, session notes, AI review"
```

---

### Task 4: System prompt update, CHANGELOG, and final verification

**Files:**
- Modify: `src/main/java/com/notare/sage/chat/SageChatService.java`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: nothing new — this task only touches a string constant and documentation.
- Produces: nothing new consumed by other tasks (this is the last task in this plan).

- [ ] **Step 1: Update `CHAT_SYSTEM_PROMPT`**

In `SageChatService.java`, replace the existing `CHAT_SYSTEM_PROMPT` block:

```java
    private static final String CHAT_SYSTEM_PROMPT = """
            You are Sage, an AI assistant embedded in a tutoring platform, talking directly with a \
            tutor. You have tools to look up students, courses, assignments, submissions, and \
            sessions, and tools to take actions (release feedback, schedule/complete a session, \
            post an announcement). Always resolve an ambiguous name (a student, a course) via a \
            list/get tool before acting or answering - never guess an ID. When you call a write \
            tool, the system will pause for the tutor's confirmation automatically; you do not \
            need to ask them to confirm in your own text, but you may briefly explain what you're \
            about to do. Be concise.""";
```

with:

```java
    private static final String CHAT_SYSTEM_PROMPT = """
            You are Sage, an AI assistant embedded in a tutoring platform, talking directly with a \
            tutor. You have tools to look up students, courses, assignments, submissions, sessions, \
            session notes, materials, topics, grade categories, rubrics, and pending reviews, and \
            tools to take actions (release feedback, schedule/complete a session, post an \
            announcement, update a course, create an assignment/material/topic/grade category, \
            rename a topic, update a grade category, save session notes, set rubric scores, draft \
            session notes, and review a submission). Always resolve an ambiguous name or ID (a \
            student, a course, a topic, a grade category, a rubric criterion) via a list/get tool \
            before acting or answering - never guess an ID. When updating an existing course or \
            grade category, look up its current values first and carry forward any field you are \
            not changing - the update replaces the whole record, not just the field you mention. \
            When you call a write tool, the system will pause for the tutor's confirmation \
            automatically; you do not need to ask them to confirm in your own text, but you may \
            briefly explain what you're about to do. Be concise.""";
```

- [ ] **Step 2: Compile**

```bash
./mvnw.cmd -q compile
```

Expected: no output, exit code 0.

- [ ] **Step 3: Update `CHANGELOG.md`**

Read the two most recent existing entries first to match tone/format/level of detail, then add a
new dated entry (today's date) at the top summarizing: the 8 new read tools and 11 new write tools
by name, the one new array-parameter schema shape, that `WRITE_TOOL_NAMES` is now 15 entries, and
that verification was performed via the temporary debug-endpoint technique described in this plan
(never committed) rather than a live model-driven curl flow, since this environment still has no
reachable `ANTHROPIC_API_KEY` — same documented gap as the original feature's Task 6/9.

- [ ] **Step 4: Full backend compile and a final debug-endpoint smoke test**

```bash
./mvnw.cmd -q compile
```

Expected: no output, exit code 0. Re-confirm via `grep` that no stray `DebugSageToolController.java`
or throwaway `main` method survived in the working tree from Tasks 1-3:

```bash
git status --short
find src/main/java/com/notare/sage/chat -name "DebugSageToolController.java"
```

Expected: `git status --short` shows nothing uncommitted, and the `find` returns nothing.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/notare/sage/chat/SageChatService.java CHANGELOG.md
git commit -m "Update Sage chat system prompt for expanded tool set; document the expansion in CHANGELOG"
```

---

## Self-Review Notes

**Spec coverage:** every read tool and write tool named in
`docs/superpowers/specs/2026-09-10-sage-chat-tool-expansion-design.md`'s two tables has a task
above implementing it — 8/8 read tools (Task 2), 11/11 write tools (Task 3). The "stays UI-only"
list has no corresponding task, by design. The system-prompt update (Task 4) covers the spec's
"look up current values before a partial update" requirement. The array-param builder (Task 1) is
the one new mechanism the spec called out, and its JSON-Schema shape here matches the spec's
example verbatim.

**Type consistency check performed:** every new `SageToolExecutor` private method's return type
matches the DTO its delegated-to service method actually returns (`CourseService.updateCourse` →
`CourseResponse`, `AssignmentService.createAssignment` → `AssignmentResponse`, etc. — verified
against each service's real method signature during plan authoring, not assumed). Every new tool
name used in Task 1's `ALL_TOOLS` entries is the exact same string switched on in Task 2/Task 3's
`execute()`/`describeAction()` — cross-checked list by list while writing this plan, not just
individually plausible.

**One thing this plan does not attempt:** a real, live, model-driven exercise of any new tool
through `POST /api/sage/chat` — the same `ANTHROPIC_API_KEY` gap that blocked part of the original
feature's Task 6/9 still applies. The temporary debug-endpoint technique verifies every new tool's
actual behavior (ownership checks, real mutations, real descriptions) without going through the
Anthropic API, which is the best available substitute in this environment — but it does not prove
the model will *choose* to call these tools correctly from natural language. That risk is
inherent to every tool in this feature, old and new alike, and was already accepted (and separately
verified as address-able only with a real key) for the original 14 tools.
