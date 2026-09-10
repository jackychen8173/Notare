package com.notare.sage.chat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import com.notare.sage.chat.block.BlockConverter;
import com.notare.sage.chat.block.StoredBlock;
import com.notare.sage.chat.dto.ChatTurnResponse;
import com.notare.sage.chat.dto.PendingAction;
import com.notare.sage.chat.dto.SageConversationSummaryResponse;
import com.notare.sage.chat.dto.SageMessageResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SageChatService {

    private static final Model MODEL = Model.CLAUDE_SONNET_4_6;
    private static final int MAX_TOOL_ROUNDTRIPS = 6;

    private static final String CHAT_SYSTEM_PROMPT = """
            You are Sage, an AI assistant embedded in a tutoring platform, talking directly with a \
            tutor. You have tools to look up students, courses, assignments, submissions, and \
            sessions, and tools to take actions (release feedback, schedule/complete a session, \
            post an announcement). Always resolve an ambiguous name (a student, a course) via a \
            list/get tool before acting or answering - never guess an ID. When you call a write \
            tool, the system will pause for the tutor's confirmation automatically; you do not \
            need to ask them to confirm in your own text, but you may briefly explain what you're \
            about to do. Be concise.""";

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    private final SageConversationRepository conversationRepository;
    private final SageMessageRepository messageRepository;
    private final SageToolExecutor toolExecutor;
    private final UserRepository userRepository;

    public SageChatService(
            AnthropicClient anthropicClient,
            ObjectMapper objectMapper,
            SageConversationRepository conversationRepository,
            SageMessageRepository messageRepository,
            SageToolExecutor toolExecutor,
            UserRepository userRepository
    ) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.toolExecutor = toolExecutor;
        this.userRepository = userRepository;
    }

    public ChatTurnResponse sendMessage(UUID conversationId, String userText, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        SageConversation conversation = conversationId != null
                ? requireOwnedConversation(conversationId, tutor)
                : createConversation(tutor);

        appendMessage(conversation, MessageRole.USER, List.of(new StoredBlock.Text(userText)), null, null);

        List<SageMessage> newMessages = runLoop(conversation, tutor, 0);
        return toChatTurnResponse(conversation.getId(), newMessages);
    }

    public ChatTurnResponse confirmAction(UUID conversationId, UUID messageId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        SageConversation conversation = requireOwnedConversation(conversationId, tutor);
        SageMessage pendingMessage = requirePendingMessage(conversation, messageId);
        PendingAction action = readPendingAction(pendingMessage);

        String resultJson;
        boolean isError = false;
        try {
            resultJson = toolExecutor.execute(action.toolName(), action.input(), tutor);
        } catch (ResponseStatusException e) {
            resultJson = String.valueOf(e.getReason());
            isError = true;
        }

        pendingMessage.setActionStatus(ActionStatus.CONFIRMED);
        messageRepository.save(pendingMessage);

        appendMessage(conversation, MessageRole.TOOL,
                List.of(new StoredBlock.ToolResult(action.toolUseId(), resultJson, isError)), null, null);

        List<SageMessage> newMessages = runLoop(conversation, tutor, 0);
        return toChatTurnResponse(conversation.getId(), newMessages);
    }

    public ChatTurnResponse declineAction(UUID conversationId, UUID messageId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        SageConversation conversation = requireOwnedConversation(conversationId, tutor);
        SageMessage pendingMessage = requirePendingMessage(conversation, messageId);
        PendingAction action = readPendingAction(pendingMessage);

        pendingMessage.setActionStatus(ActionStatus.DECLINED);
        messageRepository.save(pendingMessage);

        appendMessage(conversation, MessageRole.TOOL,
                List.of(new StoredBlock.ToolResult(action.toolUseId(), "The tutor declined this action.", false)),
                null, null);

        List<SageMessage> newMessages = runLoop(conversation, tutor, 0);
        return toChatTurnResponse(conversation.getId(), newMessages);
    }

    @Transactional(readOnly = true)
    public List<SageConversationSummaryResponse> listConversations(String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        return conversationRepository.findByTutorIdOrderByUpdatedAtDesc(tutor.getId()).stream()
                .map(conversation -> {
                    List<SageMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
                    String preview = messages.stream()
                            .filter(m -> m.getRole() == MessageRole.USER)
                            .findFirst()
                            .map(m -> SageMessageResponse.from(m, objectMapper).text())
                            .orElse("New conversation");
                    return SageConversationSummaryResponse.from(conversation, preview);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SageMessageResponse> getConversation(UUID conversationId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        SageConversation conversation = requireOwnedConversation(conversationId, tutor);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(m -> SageMessageResponse.from(m, objectMapper))
                .toList();
    }

    // ---- the loop ----

    private List<SageMessage> runLoop(SageConversation conversation, User tutor, int depth) {
        if (depth >= MAX_TOOL_ROUNDTRIPS) {
            SageMessage capped = appendMessage(conversation, MessageRole.ASSISTANT,
                    List.of(new StoredBlock.Text(
                            "I've made several lookups but still need more information - could you narrow down your question?")),
                    null, null);
            return List.of(capped);
        }

        List<SageMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<MessageParam> apiHistory = buildApiHistory(history);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(2048L)
                .system(CHAT_SYSTEM_PROMPT)
                .messages(apiHistory)
                .tools(SageToolDefinitions.ALL_TOOLS.stream().map(ToolUnion::ofTool).toList())
                .build();

        Message response;
        try {
            response = anthropicClient.messages().create(params);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sage is unavailable right now", e);
        }

        List<StoredBlock> responseBlocks = BlockConverter.fromApiResponse(response.content());

        List<ToolUseBlock> toolUses = response.content().stream()
                .filter(ContentBlock::isToolUse)
                .map(ContentBlock::asToolUse)
                .toList();

        Optional<ToolUseBlock> writeCall = toolUses.stream()
                .filter(t -> SageToolExecutor.WRITE_TOOL_NAMES.contains(t.name()))
                .findFirst();

        if (writeCall.isPresent()) {
            ToolUseBlock call = writeCall.get();
            Map<String, Object> input = call._input().convert(new TypeReference<Map<String, Object>>() {
            });
            String description = toolExecutor.describeAction(call.name(), input, tutor);
            PendingAction pendingAction = new PendingAction(call.name(), call.id(), input, description);
            SageMessage assistantMessage = appendMessage(conversation, MessageRole.ASSISTANT, responseBlocks,
                    ActionStatus.PENDING, pendingAction);
            return List.of(assistantMessage);
        }

        SageMessage assistantMessage = appendMessage(conversation, MessageRole.ASSISTANT, responseBlocks, null, null);

        if (toolUses.isEmpty()) {
            return List.of(assistantMessage);
        }

        List<StoredBlock> resultBlocks = new ArrayList<>();
        for (ToolUseBlock call : toolUses) {
            Map<String, Object> input = call._input().convert(new TypeReference<Map<String, Object>>() {
            });
            try {
                String result = toolExecutor.execute(call.name(), input, tutor);
                resultBlocks.add(new StoredBlock.ToolResult(call.id(), result, false));
            } catch (ResponseStatusException e) {
                resultBlocks.add(new StoredBlock.ToolResult(call.id(), String.valueOf(e.getReason()), true));
            }
        }
        SageMessage toolMessage = appendMessage(conversation, MessageRole.TOOL, resultBlocks, null, null);

        List<SageMessage> produced = new ArrayList<>();
        produced.add(assistantMessage);
        produced.add(toolMessage);
        produced.addAll(runLoop(conversation, tutor, depth + 1));
        return produced;
    }

    private List<MessageParam> buildApiHistory(List<SageMessage> messages) {
        List<MessageParam> history = new ArrayList<>();
        for (SageMessage message : messages) {
            List<StoredBlock> blocks = BlockConverter.deserialize(message.getContent(), objectMapper);
            MessageParam.Role role = message.getRole() == MessageRole.ASSISTANT
                    ? MessageParam.Role.ASSISTANT
                    : MessageParam.Role.USER; // USER and TOOL both map to API role "user"
            history.add(MessageParam.builder()
                    .role(role)
                    .contentOfBlockParams(BlockConverter.toApiParams(blocks))
                    .build());
        }
        return history;
    }

    // ---- persistence helpers ----

    private SageConversation createConversation(User tutor) {
        SageConversation conversation = SageConversation.builder().tutor(tutor).build();
        conversationRepository.save(conversation);
        return conversation;
    }

    private SageMessage appendMessage(
            SageConversation conversation, MessageRole role, List<StoredBlock> blocks,
            ActionStatus actionStatus, PendingAction pendingAction
    ) {
        SageMessage message = SageMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(BlockConverter.serialize(blocks, objectMapper))
                .actionStatus(actionStatus)
                .pendingAction(pendingAction != null ? writePendingActionJson(pendingAction) : null)
                .build();
        messageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return message;
    }

    private String writePendingActionJson(PendingAction action) {
        try {
            return objectMapper.writeValueAsString(action);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize pending action", e);
        }
    }

    private PendingAction readPendingAction(SageMessage message) {
        try {
            return objectMapper.readValue(message.getPendingAction(), PendingAction.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupt pending action JSON", e);
        }
    }

    private ChatTurnResponse toChatTurnResponse(UUID conversationId, List<SageMessage> messages) {
        return new ChatTurnResponse(conversationId,
                messages.stream().map(m -> SageMessageResponse.from(m, objectMapper)).toList());
    }

    // ---- ownership checks ----

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private SageConversation requireOwnedConversation(UUID conversationId, User tutor) {
        SageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's conversation exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        return conversation;
    }

    private SageMessage requirePendingMessage(SageConversation conversation, UUID messageId) {
        SageMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!message.getConversation().getId().equals(conversation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
        if (message.getActionStatus() != ActionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This action is not pending confirmation");
        }
        return message;
    }
}
