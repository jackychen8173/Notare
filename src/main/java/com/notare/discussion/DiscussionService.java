package com.notare.discussion;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.discussion.dto.CreatePostRequest;
import com.notare.discussion.dto.CreateThreadRequest;
import com.notare.discussion.dto.DiscussionAuthor;
import com.notare.discussion.dto.DiscussionPostResponse;
import com.notare.discussion.dto.DiscussionThreadDetail;
import com.notare.discussion.dto.DiscussionThreadSummary;
import com.notare.discussion.dto.ModerateThreadRequest;
import com.notare.discussion.dto.UpdatePostRequest;
import com.notare.discussion.dto.UpdateThreadRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Course discussions. Two independent privacy rules, each enforced in exactly one place:
 * <ul>
 *   <li>PRIVATE threads: {@link DiscussionThreadRepository#findVisibleToStudent} /
 *       {@link DiscussionThreadRepository#findByIdVisibleToStudent} - every student read path goes
 *       through those queries, so another student's private thread is simply a 404.</li>
 *   <li>Anonymous authors: {@link DiscussionAuthor#forStudent} - every student response is built
 *       through it, so the name never leaves the server for anyone but the tutor and the author.</li>
 * </ul>
 */
@Service
@Transactional
public class DiscussionService {

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionPostRepository postRepository;
    private final DiscussionThreadReadRepository readRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public DiscussionService(
            DiscussionThreadRepository threadRepository,
            DiscussionPostRepository postRepository,
            DiscussionThreadReadRepository readRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.readRepository = readRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // ---- Tutor ----

    @Transactional(readOnly = true)
    public List<DiscussionThreadSummary> listThreadsForTutor(UUID courseId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        Course course = requireOwnedCourse(courseId, tutor);
        List<DiscussionThread> threads = threadRepository.findByCourseIdOrderByPinnedDescLastActivityAtDesc(course.getId());
        return summarize(threads, tutor, (author, anonymous) -> DiscussionAuthor.forTutor(author, anonymous, tutor.getId()));
    }

    public DiscussionThreadDetail createThreadAsTutor(UUID courseId, CreateThreadRequest request, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        Course course = requireOwnedCourse(courseId, tutor);
        requireNotArchived(course);

        // A tutor's thread is always a public, named post - private threads are student-initiated only.
        DiscussionThread thread = saveNewThread(course, tutor, request.title(), request.body(),
                DiscussionVisibility.PUBLIC, false);
        return tutorDetail(thread, tutor);
    }

    public DiscussionThreadDetail getThreadForTutor(UUID threadId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        readRepository.markRead(thread.getId(), tutor.getId(), LocalDateTime.now());
        return tutorDetail(thread, tutor);
    }

    public DiscussionPostResponse replyAsTutor(UUID threadId, CreatePostRequest request, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        requireNotArchived(thread.getCourse());
        // Locking stops student replies only - the tutor can still close out a locked thread.
        DiscussionPost post = saveNewPost(thread, tutor, request.body(), false);
        return DiscussionPostResponse.from(post, DiscussionAuthor.forTutor(tutor, false, tutor.getId()));
    }

    public DiscussionThreadDetail updateThreadAsTutor(UUID threadId, UpdateThreadRequest request, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        requireAuthor(thread.getAuthor(), tutor);
        applyThreadEdit(thread, request);
        return tutorDetail(thread, tutor);
    }

    public DiscussionThreadDetail moderateThread(UUID threadId, ModerateThreadRequest request, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        if (request.pinned() != null) {
            thread.setPinned(request.pinned());
        }
        if (request.locked() != null) {
            thread.setLocked(request.locked());
        }
        return tutorDetail(thread, tutor);
    }

    /**
     * Turns a student's private question into a public thread. The student shared their name only with
     * the tutor, so their opening post and replies are flipped to anonymous - making it public must never
     * expose a name to classmates that the student didn't choose to share. Tutor replies stay named.
     */
    public DiscussionThreadDetail makeThreadPublic(UUID threadId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        if (thread.getVisibility() == DiscussionVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thread is already public");
        }

        User author = thread.getAuthor();
        thread.setVisibility(DiscussionVisibility.PUBLIC);
        if (author.getRole() == UserRole.STUDENT) {
            thread.setAnonymous(true);
            for (DiscussionPost post : postRepository.findByThreadIdAndAuthorId(thread.getId(), author.getId())) {
                post.setAnonymous(true);
            }
        }
        // Bump activity so it surfaces as new/unread for the rest of the class.
        LocalDateTime now = LocalDateTime.now();
        thread.setLastActivityAt(now);
        readRepository.markRead(thread.getId(), tutor.getId(), now);
        return tutorDetail(thread, tutor);
    }

    public void deleteThreadAsTutor(UUID threadId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionThread thread = requireOwnedThread(threadId, tutor);
        threadRepository.delete(thread);
    }

    public DiscussionPostResponse updatePostAsTutor(UUID postId, UpdatePostRequest request, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionPost post = requirePost(postId);
        requireOwnedThread(post.getThread().getId(), tutor);
        requireAuthor(post.getAuthor(), tutor);
        applyPostEdit(post, request);
        return DiscussionPostResponse.from(post, DiscussionAuthor.forTutor(post.getAuthor(), post.isAnonymous(), tutor.getId()));
    }

    /** Moderation: a tutor can delete any reply in their own course, not just their own. */
    public void deletePostAsTutor(UUID postId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        DiscussionPost post = requirePost(postId);
        requireOwnedThread(post.getThread().getId(), tutor);
        postRepository.delete(post);
    }

    // ---- Student ----

    @Transactional(readOnly = true)
    public List<DiscussionThreadSummary> listThreadsForStudent(UUID courseId, String studentEmail) {
        User student = requireStudent(studentEmail);
        Course course = requireEnrolledCourse(courseId, student);
        List<DiscussionThread> threads = threadRepository.findVisibleToStudent(course.getId(), student.getId());
        return summarize(threads, student, (author, anonymous) -> DiscussionAuthor.forStudent(author, anonymous, student.getId()));
    }

    public DiscussionThreadDetail createThreadAsStudent(UUID courseId, CreateThreadRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        Course course = requireEnrolledCourse(courseId, student);
        requireNotArchived(course);

        DiscussionVisibility visibility = request.visibility() != null ? request.visibility() : DiscussionVisibility.PUBLIC;
        // Anonymity only means something to classmates; a private thread is only ever seen by the tutor.
        boolean anonymous = visibility == DiscussionVisibility.PUBLIC && Boolean.TRUE.equals(request.anonymous());
        DiscussionThread thread = saveNewThread(course, student, request.title(), request.body(), visibility, anonymous);
        return studentDetail(thread, student);
    }

    public DiscussionThreadDetail getThreadForStudent(UUID threadId, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionThread thread = requireVisibleThread(threadId, student);
        readRepository.markRead(thread.getId(), student.getId(), LocalDateTime.now());
        return studentDetail(thread, student);
    }

    public DiscussionPostResponse replyAsStudent(UUID threadId, CreatePostRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionThread thread = requireVisibleThread(threadId, student);
        requireNotArchived(thread.getCourse());
        if (thread.isLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This thread is locked");
        }

        boolean anonymous = thread.getVisibility() == DiscussionVisibility.PUBLIC && Boolean.TRUE.equals(request.anonymous());
        DiscussionPost post = saveNewPost(thread, student, request.body(), anonymous);
        return DiscussionPostResponse.from(post, DiscussionAuthor.forStudent(student, anonymous, student.getId()));
    }

    public DiscussionThreadDetail updateThreadAsStudent(UUID threadId, UpdateThreadRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionThread thread = requireVisibleThread(threadId, student);
        requireAuthor(thread.getAuthor(), student);
        applyThreadEdit(thread, request);
        return studentDetail(thread, student);
    }

    /** A student can delete their own thread only while nobody has replied - otherwise replies would vanish with it. */
    public void deleteThreadAsStudent(UUID threadId, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionThread thread = requireVisibleThread(threadId, student);
        requireAuthor(thread.getAuthor(), student);
        if (postRepository.countByThreadId(thread.getId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't delete a thread that already has replies");
        }
        threadRepository.delete(thread);
    }

    public DiscussionPostResponse updatePostAsStudent(UUID postId, UpdatePostRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionPost post = requirePost(postId);
        requireVisibleThread(post.getThread().getId(), student);
        requireAuthor(post.getAuthor(), student);
        applyPostEdit(post, request);
        return DiscussionPostResponse.from(post, DiscussionAuthor.forStudent(post.getAuthor(), post.isAnonymous(), student.getId()));
    }

    public void deletePostAsStudent(UUID postId, String studentEmail) {
        User student = requireStudent(studentEmail);
        DiscussionPost post = requirePost(postId);
        requireVisibleThread(post.getThread().getId(), student);
        requireAuthor(post.getAuthor(), student);
        postRepository.delete(post);
    }

    // ---- Shared ----

    private DiscussionThread saveNewThread(Course course, User author, String title, String body,
                                           DiscussionVisibility visibility, boolean anonymous) {
        DiscussionThread thread = DiscussionThread.builder()
                .course(course)
                .author(author)
                .visibility(visibility)
                .title(title.trim())
                .body(body)
                .anonymous(anonymous)
                .build();
        // Flushed before the native read-marker upsert below, which references this row by FK.
        threadRepository.saveAndFlush(thread);
        readRepository.markRead(thread.getId(), author.getId(), thread.getLastActivityAt());
        return thread;
    }

    private DiscussionPost saveNewPost(DiscussionThread thread, User author, String body, boolean anonymous) {
        LocalDateTime now = LocalDateTime.now();
        DiscussionPost post = DiscussionPost.builder()
                .thread(thread)
                .author(author)
                .body(body)
                .anonymous(anonymous)
                .createdAt(now)
                .build();
        postRepository.saveAndFlush(post);
        thread.setLastActivityAt(now);
        // Your own reply shouldn't show up as unread to you.
        readRepository.markRead(thread.getId(), author.getId(), now);
        return post;
    }

    private void applyThreadEdit(DiscussionThread thread, UpdateThreadRequest request) {
        thread.setTitle(request.title().trim());
        thread.setBody(request.body());
        thread.setEditedAt(LocalDateTime.now());
    }

    private void applyPostEdit(DiscussionPost post, UpdatePostRequest request) {
        post.setBody(request.body());
        post.setEditedAt(LocalDateTime.now());
    }

    private DiscussionThreadDetail tutorDetail(DiscussionThread thread, User tutor) {
        return detail(thread, (author, anonymous) -> DiscussionAuthor.forTutor(author, anonymous, tutor.getId()));
    }

    private DiscussionThreadDetail studentDetail(DiscussionThread thread, User student) {
        return detail(thread, (author, anonymous) -> DiscussionAuthor.forStudent(author, anonymous, student.getId()));
    }

    private DiscussionThreadDetail detail(DiscussionThread thread, BiFunction<User, Boolean, DiscussionAuthor> authorView) {
        List<DiscussionPostResponse> posts = postRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId()).stream()
                .map(post -> DiscussionPostResponse.from(post, authorView.apply(post.getAuthor(), post.isAnonymous())))
                .toList();
        return DiscussionThreadDetail.from(thread, authorView.apply(thread.getAuthor(), thread.isAnonymous()), posts);
    }

    private List<DiscussionThreadSummary> summarize(List<DiscussionThread> threads, User viewer,
                                                    BiFunction<User, Boolean, DiscussionAuthor> authorView) {
        if (threads.isEmpty()) {
            return List.of();
        }
        List<UUID> threadIds = threads.stream().map(DiscussionThread::getId).toList();

        Map<UUID, Long> replyCounts = new HashMap<>();
        for (Object[] row : postRepository.countByThreadIds(threadIds)) {
            replyCounts.put((UUID) row[0], (Long) row[1]);
        }
        Map<UUID, LocalDateTime> lastReads = new HashMap<>();
        for (DiscussionThreadRead read : readRepository.findByUserIdAndThreadIdIn(viewer.getId(), threadIds)) {
            lastReads.put(read.getThread().getId(), read.getLastReadAt());
        }

        return threads.stream()
                .map(thread -> {
                    LocalDateTime lastRead = lastReads.get(thread.getId());
                    boolean unread = lastRead == null || thread.getLastActivityAt().isAfter(lastRead);
                    return DiscussionThreadSummary.from(
                            thread,
                            authorView.apply(thread.getAuthor(), thread.isAnonymous()),
                            replyCounts.getOrDefault(thread.getId(), 0L),
                            unread);
                })
                .toList();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void requireAuthor(User author, User viewer) {
        if (!author.getId().equals(viewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change your own posts");
        }
    }

    private void requireNotArchived(Course course) {
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot post to an archived course");
        }
    }

    private Course requireOwnedCourse(UUID courseId, User tutor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's course exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Course requireEnrolledCourse(UUID courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private DiscussionThread requireOwnedThread(UUID threadId, User tutor) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

        if (!thread.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's thread exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }

        return thread;
    }

    private DiscussionThread requireVisibleThread(UUID threadId, User student) {
        // Someone else's private thread is indistinguishable from a nonexistent one.
        DiscussionThread thread = threadRepository.findByIdVisibleToStudent(threadId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), thread.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }

        return thread;
    }

    private DiscussionPost requirePost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
