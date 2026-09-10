package com.notare.topic;

import com.notare.common.ApiResponse;
import com.notare.topic.dto.CreateTopicRequest;
import com.notare.topic.dto.RenameTopicRequest;
import com.notare.topic.dto.TopicResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping("/api/courses/{courseId}/topics")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateTopicRequest request,
            Authentication authentication
    ) {
        TopicResponse response = topicService.createTopic(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> listTopics(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(topicService.listTopics(courseId, authentication.getName())));
    }

    @PatchMapping("/api/topics/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> renameTopic(
            @PathVariable UUID id,
            @Valid @RequestBody RenameTopicRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(topicService.renameTopic(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/topics/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        topicService.deleteTopic(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Topic deleted", null));
    }
}
