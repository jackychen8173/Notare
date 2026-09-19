# Sage Chat: Tool Expansion Design Spec

Date: 2026-09-10
Status: Approved by user, pending implementation plan

## Purpose

Sage Chat (`com.notare.sage.chat`, shipped this session) currently exposes 10 read tools and 4
write tools — enough to look up students/courses/assignments/submissions/sessions and to release
feedback, schedule/complete a session, or post an announcement. The original design spec explicitly
deferred every other mutation in the app ("course/assignment/topic/material/grade-category/rubric
creation, removing a student, etc.") as out of scope, picked from a multi-select question rather
than an exhaustive sweep.

Since then, the app grew a full course-editing surface (topics, materials, grade categories,
rubrics, course archiving — commit `53d4b06`) that Sage still can't see or touch at all, and three
pre-existing standalone AI features (`draft-notes`, `review-submission`, `pending-reviews`,
`com.notare.sage.SageController`) that were never wired into the chat loop either. The user's
complaint that triggered this spec: Sage's one shipped-but-orphaned write action
(`schedule_session`) has no matching visible affordance anywhere *except* the manual "Schedule
session" button on the student page it happens to duplicate — highlighting that Sage's tool set and
the app's actual current feature set have drifted apart. This spec closes that gap: Sage should be
able to read and (mostly) act on everything the app's UI can already do.

## Scope decisions (from user Q&A during brainstorming)

- **Expand, don't just wire up UI:** the fix is adding real tool coverage for the app's current
  feature set, not just adding "Ask Sage" buttons around the existing 14 tools.
- **Some writes stay UI-only:** destructive, rare, or structurally-complex-for-chat mutations get a
  read tool (where applicable) but no write tool — the tutor keeps using the existing button/form
  for these. Decided per-action below, not by a single blanket rule.
- **The three legacy standalone AI features get wired in too:** `draft-notes` and
  `review-submission` become confirm-gated write tools (both already mutate real rows today, just
  behind a manual button click instead of a chat confirm step — see below); `pending-reviews`
  becomes a plain read tool.
- **No new mechanism.** Every addition slots into the existing `SageToolDefinitions` /
  `SageToolExecutor` / `SageChatService.WRITE_TOOL_NAMES` registry pattern already built. The one
  new piece of infrastructure needed: `SageToolDefinitions`'s tool-builder only supports flat
  `{type, description}` parameters today (see `SageToolDefinitions.java:17-26`); `update_rubric_scores`
  needs an `array`-of-objects parameter, so the builder needs a second entry point for that one
  shape (see below).

## Why draft_session_notes / review_submission are write tools, not read tools

`SageService.draftSessionNotes` (`SageService.java:85-96`) overwrites `note.setFormattedNotes(...)`
and saves; `SageService.reviewSubmission` (`SageService.java:98-115`) overwrites
`submission.setSageFeedback(...)` and saves. Neither is visible to a student on its own — formatted
notes are tutor-only, and `sageFeedback` is never shown to a student until a separate
`release_feedback` call sets `releasedAt` — but both are real, persisted mutations. The existing
manual UI treats one button click as sufficient authorization for that mutation; in chat, the
*model* decides when to call the tool, so it needs the same propose-then-confirm gate as every
other Sage-chat write action, for the same trust-boundary reason `schedule_session` etc. do.

## New read tools (execute immediately, no confirmation)

| Tool | Input | Delegates to |
|---|---|---|
| `get_session` | `sessionId: string` | `SessionService.getSession` (already tutor-scoped) |
| `get_session_notes` | `sessionId: string` | `SessionService.getSessionNotes` |
| `list_materials` | `courseId: string` | `MaterialService.listMaterials` |
| `list_topics` | `courseId: string` | `TopicService.listTopics` |
| `get_rubric` | `assignmentId: string` | `RubricService.getRubric` (keyed by assignment, not a separate rubric ID — matches the existing `GET /api/assignments/{id}/rubric` route) |
| `list_grade_categories` | `courseId: string` | `GradeCategoryService.listCategories` |
| `list_enrolled_students` | `courseId: string` | `CourseService.listEnrolledStudents` (distinct from existing `list_students`, which is *all* of the tutor's students across every course) |
| `list_pending_reviews` | *(none)* | `SageService.pendingReviewsCount` — returns `{count}`, matching `PendingReviewsResponse`'s existing shape (a count, not a list of specific submissions) |

All of these delegate straight to an existing service method that already does its own
tutor-ownership check by email (verified directly against `TopicService`, `SessionService`,
`CourseService` source — same `requireOwnedX`-by-email pattern already used by
`release_feedback`/`complete_session`/`post_announcement`) — no new ownership-check helpers needed
in `SageToolExecutor` for these.

## New write tools (pause loop, require confirmation)

| Tool | Input | Delegates to |
|---|---|---|
| `update_course` | `courseId: string, name: string, subject: string, description?: string` | `CourseService.updateCourse` (`UpdateCourseRequest` requires `name`+`subject` together — see system-prompt note below) |
| `create_assignment` | `courseId: string, title: string, description?: string, dueDate: string (ISO date), topicId?: string, gradeCategoryId?: string` | `AssignmentService.createAssignment` |
| `create_material` | `courseId: string, title: string, description?: string, url?: string, topicId?: string` | `MaterialService.createMaterial` |
| `create_topic` | `courseId: string, name: string` | `TopicService.createTopic` |
| `rename_topic` | `topicId: string, name: string` | `TopicService.renameTopic` |
| `create_grade_category` | `courseId: string, name: string, weightPercent: number (0-100)` | `GradeCategoryService.createCategory` |
| `update_grade_category` | `categoryId: string, name: string, weightPercent: number (0-100)` | `GradeCategoryService.updateCategory` (also requires both fields together) |
| `save_session_notes` | `sessionId: string, rawNotes: string` | `SessionService.saveSessionNotes` |
| `update_rubric_scores` | `submissionId: string, scores: array<{criterionId: string, pointsAwarded: number}>` | `SubmissionService.updateRubricScores` — needs criterion IDs, which Sage must resolve via `get_rubric`/`get_submission` first (same "never guess an ID" rule as every other tool) |
| `draft_session_notes` | `sessionId: string` | `SageService.draftSessionNotes` |
| `review_submission` | `submissionId: string` | `SageService.reviewSubmission` |

`WRITE_TOOL_NAMES` grows to 15 entries. All new write tools follow the exact same
`describeAction`/`execute` split already established: `describeAction` builds a human-readable
confirmation string (no tool-execution side effects), `execute` performs the real mutation only
after `confirmAction`.

### Confirmation descriptions (binding constraint carried forward from the final review)

The final whole-branch review on the original Sage Chat feature found and fixed a real
disclosure bug: `release_feedback`'s description omitted the tutor-commentary text and
`post_announcement` truncated its content, so a tutor could confirm an action without seeing
everything it would do. Every new write tool's `describeAction` text must show the **full** value
of any free-text field the tutor is confirming — no truncation, no omission — in particular:
`save_session_notes` (full `rawNotes`), `update_course`/`create_assignment`/`create_material`/
`create_topic`/`create_grade_category` (full name/title/description text), and
`update_rubric_scores` (every criterion name + awarded/possible points, not just a total).

## New param type: array-of-objects (for `update_rubric_scores`)

`SageToolDefinitions`'s current `tool()` helper (`SageToolDefinitions.java:17-26`) builds each
parameter as a flat `{"type": ..., "description": ...}` JSON Schema object via a `Param(name, type,
description)` record — it has no way to express `"type": "array"` with an `"items"` sub-schema.
`update_rubric_scores` needs exactly that shape:

```json
"scores": {
  "type": "array",
  "description": "The rubric criterion scores to set",
  "items": {
    "type": "object",
    "properties": {
      "criterionId": { "type": "string", "description": "The rubric criterion's UUID" },
      "pointsAwarded": { "type": "number", "description": "Points to award for this criterion" }
    },
    "required": ["criterionId", "pointsAwarded"]
  }
}
```

The plan should add a second, raw-schema entry point (e.g. `tool(name, description, Map<String,
Object> rawProperties, List<String> required)`, alongside the existing `Param`-based one, rather
than bending `Param` to express nested schemas) used only by `update_rubric_scores` — every other
new tool uses the existing flat-`Param` builder unchanged.

## Stays UI-only — no Sage write action

Read coverage is still added above where it exists (e.g. `list_materials` for course content that
can only be deleted via the UI); no *write* tool is added for:

| Action | Why UI-only |
|---|---|
| `create_course` | One-time setup with several fields chosen deliberately, not a natural mid-conversation ask |
| `archive_course` / `unarchive_course` | Significant, infrequent state change (hides/restores an entire course and its content from students) |
| `regenerate_join_code` | Destructive to the old code — any student with the old link/code loses access |
| `remove_student_from_course` | Revokes a student's access; consequential enough to want a deliberate UI click, not a chat aside |
| `delete_material` | Destructive, irreversible |
| `delete_topic` | Destructive; also cascades to any material/assignment referencing it |
| `delete_grade_category` | Destructive |
| `create_rubric` | Nested criteria + per-criterion point values don't fit a flat chat-tool schema well (same class of problem `update_rubric_scores` solves narrowly with the new array param — full rubric *creation* would need a much larger nested schema for comparatively rare, deliberate one-time setup) |
| `delete_rubric` | Destructive |
| `update_student` | Edits a student's own contact info (PII); rare to come up conversationally and worth a deliberate form |

## System prompt update

`SageChatService.CHAT_SYSTEM_PROMPT` gains one clause covering the two-required-field update
actions (`update_course`, `update_grade_category`): when only one field is changing, look up the
current values first (`get_course`, `list_grade_categories`) and carry the unchanged field(s)
forward rather than guessing or leaving them blank — reusing the prompt's existing "never guess,
always resolve via a read tool first" instruction rather than adding a new rule from scratch.

## Ownership checks

Every new tool reuses an existing service method that already checks ownership by tutor email
(`TopicService`, `MaterialService`, `GradeCategoryService`, `RubricService`, `SessionService`,
`CourseService`, `SubmissionService`, `SageService` — all follow the same 404-not-403 pattern
verified in the original spec and confirmed again here for `TopicService` directly). No new
`requireOwnedX` helper methods are needed in `SageToolExecutor` itself; every new tool is straight
delegation, the same shape already used for `complete_session`/`post_announcement`.

## No changes needed

- **Data model, loop mechanics, API surface, frontend components** — unchanged. This is purely
  more entries in `ALL_TOOLS`/`WRITE_TOOL_NAMES`/`execute`/`describeAction`, plus the one new
  raw-schema tool-builder entry point. `SageChatMessage.tsx`'s pending-action card already renders
  whatever description string the backend sends; it needs no frontend change to show a longer or
  different description.
- **Error handling** — unchanged; the existing "tool error becomes an `is_error` tool_result, never
  thrown out of the loop" contract (including the `describeAction`-specific try/catch fixed in the
  final review) already covers every new tool the same way.

## Testing / verification plan

Same curl-flow + build/lint rigor as the original feature (no automated test suite exists in this
repo — pre-existing, documented gap, not addressed here). Per new write tool: propose it, confirm
it, verify the underlying row actually changed via the corresponding existing REST GET endpoint;
propose one and decline it, verify no state change. Confirm the 404-not-403 ownership pattern holds
for at least one new read tool and one new write tool against a second tutor's data. Confirm
`update_rubric_scores`'s array param actually round-trips through the Anthropic tool-call schema
(the one genuinely new schema shape in this expansion) with a real multi-criterion payload.

## Explicitly out of scope for this spec

- Every action listed in "Stays UI-only" above.
- Any change to the confirm/decline mechanics, persistence model, or frontend page structure.
- Bulk/batch variants of any tool (e.g. creating multiple topics in one call).
