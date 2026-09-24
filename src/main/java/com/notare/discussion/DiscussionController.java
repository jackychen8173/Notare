package com.notare.discussion;

import com.notare.common.ApiResponse;
import com.notare.discussion.dto.CreatePostRequest;
import com.notare.discussion.dto.CreateThreadRequest;
import com.notare.discussion.dto.DiscussionPostResponse;
import com.notare.discussion.dto.DiscussionThreadDetail;
import com.notare.discussion.dto.DiscussionThreadSummary;
import com.notare.discussion.dto.ModerateThreadRequest;
import com.notare.discussion.dto.UpdatePostRequest;
import com.notare.discussion.dto.UpdateThreadRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/api/courses/{courseId}/discussions")
    public ResponseEntity<ApiResponse<List<DiscussionThreadSummary>>> listThreads(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.listThreadsForTutor(courseId, authentication.getName())));
    }

    @PostMapping("/api/courses/{courseId}/discussions")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> createThread(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateThreadRequest request,
            Authentication authentication
    ) {
        DiscussionThreadDetail response = discussionService.createThreadAsTutor(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/discussions/{id}")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> getThread(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.getThreadForTutor(id, authentication.getName())));
    }

    @PutMapping("/api/discussions/{id}")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> updateThread(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThreadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.updateThreadAsTutor(id, request, authentication.getName())));
    }

    @PatchMapping("/api/discussions/{id}/moderation")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> moderateThread(
            @PathVariable UUID id,
            @RequestBody ModerateThreadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.moderateThread(id, request, authentication.getName())));
    }

    @PostMapping("/api/discussions/{id}/make-public")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> makeThreadPublic(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.makeThreadPublic(id, authentication.getName())));
    }

    @DeleteMapping("/api/discussions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        discussionService.deleteThreadAsTutor(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Thread deleted", null));
    }

    @PostMapping("/api/discussions/{id}/posts")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        DiscussionPostResponse response = discussionService.replyAsTutor(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/api/discussion-posts/{id}")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.updatePostAsTutor(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/discussion-posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        discussionService.deletePostAsTutor(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }
}
