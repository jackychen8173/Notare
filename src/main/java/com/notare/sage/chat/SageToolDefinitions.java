package com.notare.sage.chat;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.util.List;
import java.util.Map;

public final class SageToolDefinitions {

    private SageToolDefinitions() {
    }

    private record Param(String name, String type, String description) {
    }

    private static Tool tool(String name, String description, List<Param> params, List<String> required) {
        Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
        for (Param p : params) {
            propsBuilder.putAdditionalProperty(p.name(),
                    JsonValue.from(Map.of("type", p.type(), "description", p.description())));
        }
        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder().properties(propsBuilder.build());
        required.forEach(schemaBuilder::addRequired);
        return Tool.builder().name(name).description(description).inputSchema(schemaBuilder.build()).build();
    }

    private static Tool toolRaw(String name, String description, Map<String, Object> properties, List<String> required) {
        Tool.InputSchema.Properties.Builder propsBuilder = Tool.InputSchema.Properties.builder();
        properties.forEach((key, schema) -> propsBuilder.putAdditionalProperty(key, JsonValue.from(schema)));
        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder().properties(propsBuilder.build());
        required.forEach(schemaBuilder::addRequired);
        return Tool.builder().name(name).description(description).inputSchema(schemaBuilder.build()).build();
    }

    public static final List<Tool> ALL_TOOLS = List.of(
            tool("list_students",
                    "List the tutor's students, optionally filtered by a name substring. "
                            + "Use this to resolve a name the tutor mentions into a student ID.",
                    List.of(new Param("query", "string", "Optional case-insensitive substring to filter names by")),
                    List.of()),
            tool("get_student", "Get a single student's detail by ID.",
                    List.of(new Param("studentId", "string", "The student's UUID")),
                    List.of("studentId")),
            tool("list_courses", "List the tutor's courses.",
                    List.of(new Param("archived", "boolean", "If true, list archived courses instead of active ones. Defaults to active.")),
                    List.of()),
            tool("get_course", "Get a single course's detail by ID.",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("list_assignments", "List the assignments in a course.",
                    List.of(new Param("courseId", "string", "The course's UUID")),
                    List.of("courseId")),
            tool("get_assignment", "Get a single assignment's detail by ID.",
                    List.of(new Param("assignmentId", "string", "The assignment's UUID")),
                    List.of("assignmentId")),
            tool("list_submissions",
                    "List submissions, optionally filtered by assignment, by student, and/or to only "
                            + "pending (not yet released) ones. At least narrow by assignmentId or studentId "
                            + "when possible to avoid returning the tutor's entire submission history.",
                    List.of(
                            new Param("assignmentId", "string", "Optional assignment UUID to filter by"),
                            new Param("studentId", "string", "Optional student UUID to filter by"),
                            new Param("pendingOnly", "boolean", "If true, only include submissions not yet released to the student")),
                    List.of()),
            tool("get_submission", "Get a single submission's detail by ID, including feedback, grade, and rubric scores.",
                    List.of(new Param("submissionId", "string", "The submission's UUID")),
                    List.of("submissionId")),
            tool("list_sessions",
                    "List the tutor's tutoring sessions, optionally filtered by student and/or to only upcoming ones.",
                    List.of(
                            new Param("studentId", "string", "Optional student UUID to filter by"),
                            new Param("upcomingOnly", "boolean", "If true, only include sessions scheduled in the future")),
                    List.of()),
            tool("get_student_progress", "Get an AI-generated progress summary for a student based on their session and submission history.",
                    List.of(new Param("studentId", "string", "The student's UUID")),
                    List.of("studentId")),
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
            tool("release_feedback",
                    "Release a submission's feedback and grade to the student, making it visible to them. "
                            + "This is a write action requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("submissionId", "string", "The submission's UUID"),
                            new Param("grade", "string", "Optional grade to set (free text, e.g. 'B+' or '18/20')"),
                            new Param("tutorFeedback", "string", "Optional tutor commentary to add alongside or instead of Sage's feedback")),
                    List.of("submissionId")),
            tool("schedule_session",
                    "Schedule a new tutoring session. This is a write action requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("studentId", "string", "The student's UUID"),
                            new Param("courseId", "string", "Optional course UUID this session belongs to"),
                            new Param("date", "string", "ISO-8601 date-time, e.g. 2026-09-15T16:00:00"),
                            new Param("subject", "string", "What the session covers"),
                            new Param("duration", "integer", "Duration in minutes")),
                    List.of("studentId", "date", "subject", "duration")),
            tool("complete_session",
                    "Mark an existing tutoring session as complete. This is a write action requiring tutor confirmation before it takes effect.",
                    List.of(new Param("sessionId", "string", "The session's UUID")),
                    List.of("sessionId")),
            tool("post_announcement",
                    "Post an announcement to a course, visible to enrolled students. This is a write action requiring tutor confirmation before it takes effect.",
                    List.of(
                            new Param("courseId", "string", "The course's UUID"),
                            new Param("content", "string", "The announcement text")),
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
                    List.of("submissionId")),
            tool("draft_quiz_answer_feedback",
                    "Ask Sage to draft a suggested score and feedback for one short answer or essay "
                            + "quiz question in a student's quiz attempt. The suggestion is stored but "
                            + "NOT shown to the student until the tutor separately calls "
                            + "release_quiz_attempt. This is a write action requiring tutor "
                            + "confirmation before it takes effect.",
                    List.of(
                            new Param("attemptId", "string", "The quiz attempt's UUID"),
                            new Param("questionId", "string", "The question's UUID")),
                    List.of("attemptId", "questionId")),
            tool("release_quiz_attempt",
                    "Release a quiz attempt's results (score, per-question feedback) to the student, "
                            + "making them visible. This is a write action requiring tutor confirmation "
                            + "before it takes effect.",
                    List.of(new Param("attemptId", "string", "The quiz attempt's UUID")),
                    List.of("attemptId"))
    );
}
