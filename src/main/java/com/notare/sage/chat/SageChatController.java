package com.notare.sage.chat;

import com.notare.common.ApiResponse;
import com.notare.sage.chat.dto.ChatTurnResponse;
import com.notare.sage.chat.dto.SageConversationSummaryResponse;
import com.notare.sage.chat.dto.SageMessageResponse;
import com.notare.sage.chat.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sage/chat")
@PreAuthorize("hasRole('TUTOR')")
public class SageChatController {

    private final SageChatService sageChatService;

    public SageChatController(SageChatService sageChatService) {
        this.sageChatService = sageChatService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatTurnResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageChatService.sendMessage(request.conversationId(), request.message(), authentication.getName())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SageConversationSummaryResponse>>> listConversations(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(sageChatService.listConversations(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<List<SageMessageResponse>>> getConversation(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(sageChatService.getConversation(id, authentication.getName())));
    }

    @PostMapping("/{id}/messages/{messageId}/confirm")
    public ResponseEntity<ApiResponse<ChatTurnResponse>> confirmAction(
            @PathVariable UUID id,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageChatService.confirmAction(id, messageId, authentication.getName())));
    }

    @PostMapping("/{id}/messages/{messageId}/decline")
    public ResponseEntity<ApiResponse<ChatTurnResponse>> declineAction(
            @PathVariable UUID id,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageChatService.declineAction(id, messageId, authentication.getName())));
    }
}
