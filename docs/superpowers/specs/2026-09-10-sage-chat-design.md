# Sage Chat: Design Spec

Date: 2026-09-10
Status: Approved by user, pending implementation plan

## Purpose

Today Sage (the AI teaching assistant, `com.notare.sage`) only offers four fixed, one-shot
functions triggered by specific buttons: draft session notes, review a submission, summarize a
student's progress, and count pending reviews. Each call is a single `messages().create()` with
the backend pre-fetching exactly the data needed and stuffing it into the prompt — Sage cannot
decide what to look up, hold a conversation, or take any action beyond writing text into an
existing field.

This spec adds a genuine conversational interface: a tutor can ask Sage a free-form question
("What's Jamie's status on Assignment 3?", "Schedule a session with Alex for Friday at 4pm"), and
Sage can use tool calls to look up real data or propose real actions, subject to tutor
confirmation for anything that changes state.

This does **not** require MCP. Tool use / function calling is a feature of the Anthropic Messages
API itself (`tools` param on `MessageCreateParams`) — the loop runs in-process in
`SageChatService`, calling existing repositories/services directly. MCP would only matter if we
wanted these tools reachable by an external MCP client, which isn't the ask here.

## Scope decisions (from user Q&A during brainstorming)

- **Read + write**: Sage can both look things up and take real actions, not read-only.
- **Confirm before acting**: any write tool call pauses the turn and requires explicit tutor
  confirmation before it executes. Read tools execute immediately, no confirmation.
- **Persisted conversations**: conversations and messages are stored (new tables), matching how
  the rest of the app persists everything else (session notes, feedback) — not an ephemeral,
  frontend-only chat.
- **Dedicated page**: a new `/sage` route with its own sidebar nav entry, not a floating widget.
- **v1 write-action set**: release submission feedback, schedule/complete a session, post an
  announcement. (More actions — course/assignment/topic/material/grade-category/rubric creation,
  removing a student, etc. — are explicitly deferred; this list came from the user picking a
  subset in a multi-select question, not from an exhaustive sweep of every existing mutation.)

## Data model

New migration `V13__create_sage_chat.sql`:

```sql
CREATE TABLE sage_conversations (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sage_conversations_tutor_id ON sage_conversations(tutor_id);

CREATE TABLE sage_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES sage_conversations(id),
    role VARCHAR(20) NOT NULL,           -- USER, ASSISTANT, TOOL
    content JSONB NOT NULL,              -- Anthropic content-block array, stored verbatim
    pending_action JSONB,                -- non-null only on an ASSISTANT message proposing a write action
    action_status VARCHAR(20),           -- PENDING, CONFIRMED, DECLINED; null if not an action message
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sage_messages_conversation_id ON sage_messages(conversation_id);
```

No separate `title` column. The frontend derives a label from the first USER message's text,
truncated — avoids a wasted extra Sage call just to name the conversation.

`content` is stored as the raw Anthropic content-block array (a JSON array of `{type: "text",
text}` / `{type: "tool_use", id, name, input}` / `{type: "tool_result", tool_use_id, content}`
objects) so replaying history to the API on the next turn is a direct deserialize, not a
translation layer.

### Entities

- `com.notare.sage.chat.SageConversation` — id, tutor (User FK), createdAt, updatedAt.
- `com.notare.sage.chat.SageMessage` — id, conversation (SageConversation FK), role (enum
  `MessageRole { USER, ASSISTANT, TOOL }`), content (JSON string, `@JdbcTypeCode(SqlTypes.JSON)`
  same pattern as `Submission.sageFeedback`), pendingAction (nullable JSON string), actionStatus
  (nullable enum `ActionStatus { PENDING, CONFIRMED, DECLINED }`), createdAt.

## Tools

All tool *implementations* live in a new `SageToolExecutor` and take the authenticated tutor's
`User` as an explicit parameter threaded from the controller — never inferred from anything the
model says — reusing the exact ownership-check methods that already exist in each domain service
(`CourseService`, `StudentService`, etc.) rather than duplicating scoping logic.

### Read tools (execute immediately, loop continues)

| Tool | Input | Behavior |
|---|---|---|
| `list_students` | `query?: string` | Tutor's students (via existing `StudentService` scoping), optional name substring filter |
| `get_student` | `studentId: string` | Student detail; 404-shaped tool error if not this tutor's |
| `list_courses` | `archived?: boolean` | Tutor's courses |
| `get_course` | `courseId: string` | Course detail |
| `list_assignments` | `courseId: string` | Assignments in a course |
| `get_assignment` | `assignmentId: string` | Assignment detail |
| `list_submissions` | `assignmentId?: string, studentId?: string, pendingOnly?: boolean` | Filtered submission list |
| `get_submission` | `submissionId: string` | Submission detail including feedback/grade/rubric scores |
| `list_sessions` | `studentId?: string, upcomingOnly?: boolean` | Tutor's sessions, filtered |
| `get_student_progress` | `studentId: string` | Delegates to existing `SageService.generateProgressSummary` |

### Write tools (pause loop, require confirmation)

| Tool | Input | Delegates to |
|---|---|---|
| `release_feedback` | `submissionId: string` | Existing submission release-feedback service method |
| `schedule_session` | `studentId: string, courseId?: string, date: string, subject: string, durationMinutes: number` | `SessionService.createSession` |
| `complete_session` | `sessionId: string` | `SessionService.completeSession` |
| `post_announcement` | `courseId: string, content: string` | `AnnouncementService.createAnnouncement` |

Tool schemas are plain JSON Schema objects passed via the Anthropic SDK's tool-definition builder,
one static list built once in `SageChatService`.

## System prompt

New constant in `SageChatService`, distinct from the four existing prompts in `SageService`:

> You are Sage, an AI assistant embedded in a tutoring platform, talking directly with a tutor.
> You have tools to look up students, courses, assignments, submissions, and sessions, and tools
> to take actions (release feedback, schedule/complete a session, post an announcement). Always
> resolve an ambiguous name (a student, a course) via a list/get tool before acting or answering —
> never guess an ID. When you call a write tool, the system will pause for the tutor's
> confirmation automatically; you do not need to ask them to confirm in your own text, but you may
> briefly explain what you're about to do. Be concise.

## Loop mechanics (`SageChatService`)

```
sendMessage(conversationId?, userText, tutorEmail):
  conversation = conversationId present ? requireOwnedConversation(...) : create new
  append USER message (content = [{type:"text", text:userText}])
  return runLoop(conversation)

runLoop(conversation, depth=0):
  if depth >= 6: append ASSISTANT text message apologizing / stopping; return
  history = all messages for conversation, translated to Anthropic Message list
  response = anthropicClient.messages().create(model=CLAUDE_SONNET_4_6, system=SAGE_CHAT_PROMPT,
                                                tools=ALL_TOOLS, messages=history)
  append ASSISTANT message with content=response blocks (pendingAction set below if applicable)
  // A single response can contain multiple tool_use blocks. If ANY of them is a write tool,
  // the whole turn pauses for confirmation -- any read tools in that same batch execute first
  // (their results are appended as TOOL messages), but the write tool is never auto-executed.
  if response has no tool_use blocks:
      return   // plain text answer, turn complete
  execute every READ tool_use block now -> append one TOOL message per result
  if response has a WRITE tool_use block:
      set that ASSISTANT message's actionStatus=PENDING,
        pendingAction={toolName, toolUseId, input, description}
      return (turn ends here, awaiting confirmation; only the first write tool_use in a batch
              is honored -- a response proposing more than one write action in a single turn
              is not expected given the system prompt, but if it happens only the first is
              staged and the rest are ignored for this turn)
  else (all tool_use blocks were read tools):
      return runLoop(conversation, depth+1)

confirmAction(conversationId, messageId, tutorEmail):
  message = requireOwnedPendingAction(conversationId, messageId, tutorEmail)
  execute the write tool named in message.pendingAction
  message.actionStatus = CONFIRMED; save
  append TOOL message with the execution result as tool_result content
  return runLoop(conversation, depth=0)   // let Sage acknowledge

declineAction(conversationId, messageId, tutorEmail):
  message = requireOwnedPendingAction(...)
  message.actionStatus = DECLINED; save
  append TOOL message with a "user declined" tool_result
  return runLoop(conversation, depth=0)
```

A tool execution error (read or write) does not throw out of the loop — it's caught and turned
into a `tool_result` with `is_error: true` so Sage can explain the failure to the tutor in its own
next turn, matching how a real tool-use API is meant to handle failures.

## API

All under `/api/sage/chat`, `@PreAuthorize("hasRole('TUTOR')")`, same 404-not-403 ownership
pattern as every other tutor-scoped resource:

- `POST /api/sage/chat` — body `{conversationId?: UUID, message: string}` → returns
  `{conversationId, messages: SageMessageResponse[]}` (only the messages appended this turn)
- `GET /api/sage/chat` — list tutor's conversations: `{id, preview, updatedAt}[]` (preview = first
  USER message text, truncated)
- `GET /api/sage/chat/{id}` — full message history
- `POST /api/sage/chat/{id}/messages/{messageId}/confirm`
- `POST /api/sage/chat/{id}/messages/{messageId}/decline`

`SageMessageResponse` exposes role, a rendered text summary (concatenated text blocks — tool_use/
tool_result blocks are not shown raw to the frontend, they're summarized, e.g. "Looked up: Jamie
Lee"), and, when present, the pending action (`toolName`, human-readable `description`,
`actionStatus`).

## Frontend

- New route `frontend/app/(tutor)/sage/page.tsx`.
- Sidebar (`components/layout/Sidebar.tsx`) gains a 4th nav item, "Ask Sage".
- `types/sageChat.ts`, `hooks/useSageChat.ts` (`useConversations`, `useConversation(id)`,
  `useSendMessage`, `useConfirmAction`, `useDeclineAction`) — same React Query + axios pattern as
  every other hook file.
- Components: a conversation list panel, a message thread (`SageChatMessage` renders text bubbles
  normally; a message with a pending action renders a card with the description and
  Confirm/Decline buttons), and an input box.

## Error handling

- Anthropic API failure (network, rate limit, etc.) → `ResponseStatusException(BAD_GATEWAY, ...)`,
  matching the existing `SageService` convention.
- Tool execution error → caught inside the loop, surfaced to the model as an error tool_result,
  not thrown to the HTTP layer (the turn still produces a normal 200 response with Sage's
  explanation).
- Loop depth cap (6) prevents a runaway read-tool chain; on hit, append an apologetic assistant
  message rather than erroring.

## Testing / verification plan

No automated test suite exists in this repo (pre-existing, documented gap — not addressed by this
feature). Verification is the same curl-flow + build/lint rigor used for every feature so far:
start a conversation, ask a question requiring a read-tool chain (e.g. resolve a student by
partial name, then get their progress), trigger a write action, confirm it and verify the
underlying state actually changed (e.g. submission's `releasedAt` gets set), trigger another write
action and decline it (verify no state change), confirm the 404-not-403 ownership pattern holds
for `/api/sage/chat/{id}` against a second tutor's conversation. Frontend: `npm run build` /
`npm run lint` clean, all routes resolve.

## Explicitly out of scope for this spec

- Any write action beyond the four listed (course/assignment/topic/material/grade-category/rubric
  mutations, removing a student, regenerating a join code, etc.).
- Editing or deleting past conversations/messages.
- Streaming responses (turn-based request/response only, matching the synchronous style of every
  other endpoint in this codebase).
- Multi-tutor/shared conversations — one conversation belongs to exactly one tutor.
