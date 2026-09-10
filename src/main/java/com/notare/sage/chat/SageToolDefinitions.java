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
                    List.of("courseId", "content"))
    );
}
