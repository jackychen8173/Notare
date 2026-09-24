package com.notare.discussion;

import com.notare.common.ApiResponse;
import com.notare.discussion.dto.CreatePostRequest;
import com.notare.discussion.dto.CreateThreadRequest;
import com.notare.discussion.dto.DiscussionPostResponse;
import com.notare.discussion.dto.DiscussionThreadDetail;
import com.notare.discussion.dto.DiscussionThreadSummary;
import com.notare.discussion.dto.UpdatePostRequest;
import com.notare.discussion.dto.UpdateThreadRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentDiscussionController {

    private final DiscussionService discussionService;

    public StudentDiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/api/student/courses/{courseId}/discussions")
    public ResponseEntity<ApiResponse<List<DiscussionThreadSummary>>> listThreads(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.listThreadsForStudent(courseId, authentication.getName())));
    }

    @PostMapping("/api/student/courses/{courseId}/discussions")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> createThread(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateThreadRequest request,
            Authentication authentication
    ) {
        DiscussionThreadDetail response = discussionService.createThreadAsStudent(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/student/discussions/{id}")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> getThread(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.getThreadForStudent(id, authentication.getName())));
    }

    @PutMapping("/api/student/discussions/{id}")
    public ResponseEntity<ApiResponse<DiscussionThreadDetail>> updateThread(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThreadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.updateThreadAsStudent(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/student/discussions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        discussionService.deleteThreadAsStudent(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Thread deleted", null));
    }

    @PostMapping("/api/student/discussions/{id}/posts")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        DiscussionPostResponse response = discussionService.replyAsStudent(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/api/student/discussion-posts/{id}")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(discussionService.updatePostAsStudent(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/student/discussion-posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        discussionService.deletePostAsStudent(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }
}
