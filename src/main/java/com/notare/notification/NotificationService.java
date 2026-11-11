package com.notare.notification;

import com.notare.announcement.Announcement;
import com.notare.announcement.AnnouncementRepository;
import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentRepository;
import com.notare.discussion.DiscussionPost;
import com.notare.discussion.DiscussionPostRepository;
import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionThreadRepository;
import com.notare.notification.NotificationsResponse.Notification;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizRepository;
import com.notare.quizattempt.AttemptStatus;
import com.notare.quizattempt.QuizAttempt;
import com.notare.quizattempt.QuizAttemptRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In-app notifications, worked out from existing rows each time they're read rather than stored:
 * nothing runs on a timer (it would keep the sleeping backend awake), scheduled assignments notify
 * the moment they open, and there's no second copy of anyone's data to keep in sync. Only what the
 * person can already see goes in: students get work that has opened in courses they're in, and
 * discussion notifications carry no author names, since classmates can post anonymously.
 */
@Service
public class NotificationService {

    /** How far back notifications go. */
    static final Duration WINDOW = Duration.ofDays(14);
    static final Duration DUE_SOON = Duration.ofHours(24);
    static final int MAX_ITEMS = 50;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AnnouncementRepository announcementRepository;
    private final DiscussionPostRepository discussionPostRepository;
    private final DiscussionThreadRepository discussionThreadRepository;

    public NotificationService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            QuizRepository quizRepository,
            SubmissionRepository submissionRepository,
            QuizAttemptRepository quizAttemptRepository,
            AnnouncementRepository announcementRepository,
            DiscussionPostRepository discussionPostRepository,
            DiscussionThreadRepository discussionThreadRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.announcementRepository = announcementRepository;
        this.discussionPostRepository = discussionPostRepository;
        this.discussionThreadRepository = discussionThreadRepository;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse notifications(String email) {
        User user = requireUser(email);
        Instant now = Instant.now();
        Instant since = now.minus(WINDOW);
        Set<NotificationKind> muted = muted(user);

        List<Notification> all = switch (user.getRole()) {
            case STUDENT -> forStudent(user, now, since);
            case TUTOR -> forTutor(user, since);
            default -> List.of();
        };
        List<Notification> items = all.stream()
                .filter(item -> !muted.contains(item.kind()))
                .sorted(Comparator.comparing(Notification::at).reversed())
                .limit(MAX_ITEMS)
                .map(item -> item.withUnread(user.getNotificationsSeenAt() == null || item.at().isAfter(user.getNotificationsSeenAt())))
                .toList();
        int unread = (int) items.stream().filter(Notification::unread).count();
        List<NotificationKind> available = NotificationKind.forRole(user.getRole());
        return new NotificationsResponse(items, unread, available,
                available.stream().filter(muted::contains).toList());
    }

    /** Everything up to now counts as read. */
    @Transactional
    public void markSeen(String email) {
        User user = requireUser(email);
        user.setNotificationsSeenAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public NotificationsResponse updateMuted(String email, List<NotificationKind> mutedKinds) {
        User user = requireUser(email);
        List<NotificationKind> allowed = NotificationKind.forRole(user.getRole());
        Set<NotificationKind> kinds = EnumSet.noneOf(NotificationKind.class);
        for (NotificationKind kind : mutedKinds == null ? List.<NotificationKind>of() : mutedKinds) {
            if (!allowed.contains(kind)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a notification you get: " + kind);
            }
            kinds.add(kind);
        }
        user.setMutedNotifications(kinds.stream().map(Enum::name).collect(Collectors.joining(",")));
        userRepository.save(user);
        return notifications(email);
    }

    // ---- students

    private List<Notification> forStudent(User student, Instant now, Instant since) {
        Map<UUID, Course> courses = enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(Enrollment::getCourse)
                .filter(course -> course.getArchivedAt() == null)
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Notification> items = new ArrayList<>();
        if (courses.isEmpty()) {
            return items;
        }

        List<Assignment> assignments = assignmentRepository.findByCourseIdIn(courses.keySet()).stream()
                .filter(assignment -> assignment.isVisibleToStudents(now))
                .toList();
        List<Quiz> quizzes = quizRepository.findByCourseIdInAndPublishedAtIsNotNull(courses.keySet());
        List<Submission> submissions = submissionRepository.findByStudentId(student.getId());
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentId(student.getId());
        Set<UUID> handedIn = new HashSet<>();
        submissions.forEach(submission -> handedIn.add(submission.getAssignment().getId()));
        attempts.forEach(attempt -> handedIn.add(attempt.getQuiz().getId()));
        Map<UUID, Assignment> assignmentById = assignments.stream().collect(Collectors.toMap(Assignment::getId, Function.identity()));
        Map<UUID, Quiz> quizById = quizzes.stream().collect(Collectors.toMap(Quiz::getId, Function.identity()));

        for (Assignment assignment : assignments) {
            Course course = courses.get(assignment.getCourse().getId());
            // Posted before V29: when isn't known, so it never shows as new.
            if (assignment.getCreatedAt() != null) {
                Instant appeared = assignment.getOpensAt() != null && assignment.getOpensAt().isAfter(assignment.getCreatedAt())
                        ? assignment.getOpensAt() : assignment.getCreatedAt();
                if (appeared.isAfter(since)) {
                    items.add(new Notification("work-" + assignment.getId(), NotificationKind.NEW_WORK,
                            "New assignment: " + assignment.getTitle(), null, course.getId(), course.getName(),
                            "/student/assignments/" + assignment.getId(), appeared, false));
                }
            }
            if (dueSoon(assignment.getDueAt(), now) && assignment.isAcceptingSubmissions(now) && !handedIn.contains(assignment.getId())) {
                items.add(new Notification("due-" + assignment.getId(), NotificationKind.DUE_SOON,
                        "Due soon: " + assignment.getTitle(), null, course.getId(), course.getName(),
                        "/student/assignments/" + assignment.getId(), assignment.getDueAt().minus(DUE_SOON), false));
            }
        }
        for (Quiz quiz : quizzes) {
            Course course = courses.get(quiz.getCourse().getId());
            Instant published = instant(quiz.getPublishedAt());
            if (published.isAfter(since)) {
                items.add(new Notification("quiz-" + quiz.getId(), NotificationKind.NEW_WORK,
                        "New quiz: " + quiz.getTitle(), null, course.getId(), course.getName(),
                        "/student/quizzes/" + quiz.getId(), published, false));
            }
            if (quiz.getDueAt() != null && dueSoon(quiz.getDueAt(), now) && !handedIn.contains(quiz.getId())) {
                items.add(new Notification("quizdue-" + quiz.getId(), NotificationKind.DUE_SOON,
                        "Quiz due soon: " + quiz.getTitle(), null, course.getId(), course.getName(),
                        "/student/quizzes/" + quiz.getId(), quiz.getDueAt().minus(DUE_SOON), false));
            }
        }

        for (Submission submission : submissions) {
            Assignment assignment = assignmentById.get(submission.getAssignment().getId());
            if (assignment == null || submission.getReleasedAt() == null) {
                continue;
            }
            Instant released = instant(submission.getReleasedAt());
            if (released.isAfter(since)) {
                Course course = courses.get(assignment.getCourse().getId());
                items.add(new Notification("feedback-" + submission.getId(), NotificationKind.FEEDBACK,
                        "Feedback returned: " + assignment.getTitle(), null, course.getId(), course.getName(),
                        "/student/assignments/" + assignment.getId(), released, false));
            }
        }
        for (QuizAttempt attempt : attempts) {
            Quiz quiz = quizById.get(attempt.getQuiz().getId());
            if (quiz == null || attempt.getReleasedAt() == null) {
                continue;
            }
            Instant released = instant(attempt.getReleasedAt());
            if (released.isAfter(since)) {
                Course course = courses.get(quiz.getCourse().getId());
                items.add(new Notification("quizscore-" + attempt.getId(), NotificationKind.FEEDBACK,
                        "Quiz returned: " + quiz.getTitle(), null, course.getId(), course.getName(),
                        "/student/quizzes/" + quiz.getId(), released, false));
            }
        }

        for (Course course : courses.values()) {
            for (Announcement announcement : announcementRepository.findByCourseIdOrderByCreatedAtDesc(course.getId())) {
                Instant posted = instant(announcement.getCreatedAt());
                if (!posted.isAfter(since)) {
                    break;
                }
                items.add(new Notification("announcement-" + announcement.getId(), NotificationKind.ANNOUNCEMENT,
                        "Announcement", preview(announcement.getContent()), course.getId(), course.getName(),
                        "/student/courses/" + course.getId(), posted, false));
            }
        }

        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, ZoneId.systemDefault());
        items.addAll(discussion(discussionPostRepository.findRepliesToThreadsStartedBy(student.getId(), sinceLocal),
                List.of(), "/student/discussions/", "in your thread"));
        return items;
    }

    // ---- tutors

    private List<Notification> forTutor(User tutor, Instant since) {
        Map<UUID, Course> courses = courseRepository.findByTutorIdAndArchivedAtIsNull(tutor.getId()).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        List<Notification> items = new ArrayList<>();
        if (courses.isEmpty()) {
            return items;
        }

        for (Submission submission : submissionRepository.findByAssignment_Course_Tutor_Id(tutor.getId())) {
            Instant handedIn = instant(submission.getSubmittedAt());
            Course course = courses.get(submission.getAssignment().getCourse().getId());
            if (course == null || !handedIn.isAfter(since)) {
                continue;
            }
            String version = submission.getAttemptNumber() > 1 ? " (version " + submission.getAttemptNumber() + ")" : "";
            items.add(new Notification("submission-" + submission.getId(), NotificationKind.SUBMISSION,
                    submission.getStudent().getName() + " handed in " + submission.getAssignment().getTitle() + version,
                    null, course.getId(), course.getName(), "/submissions/" + submission.getId() + "/review", handedIn, false));
        }
        for (Course course : courses.values()) {
            for (QuizAttempt attempt : quizAttemptRepository.findByQuiz_Course_Id(course.getId())) {
                if (attempt.getStatus() != AttemptStatus.SUBMITTED || attempt.getSubmittedAt() == null) {
                    continue;
                }
                Instant finished = instant(attempt.getSubmittedAt());
                if (finished.isAfter(since)) {
                    items.add(new Notification("attempt-" + attempt.getId(), NotificationKind.SUBMISSION,
                            attempt.getStudent().getName() + " finished " + attempt.getQuiz().getTitle(), null,
                            course.getId(), course.getName(), "/quiz-attempts/" + attempt.getId() + "/review", finished, false));
                }
            }
            for (Enrollment enrollment : enrollmentRepository.findByCourseId(course.getId())) {
                Instant joined = instant(enrollment.getEnrolledAt());
                if (joined.isAfter(since)) {
                    items.add(new Notification("joined-" + course.getId() + "-" + enrollment.getStudent().getId(),
                            NotificationKind.JOINED, enrollment.getStudent().getName() + " joined", null,
                            course.getId(), course.getName(), "/courses/" + course.getId() + "?tab=people", joined, false));
                }
            }
        }

        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, ZoneId.systemDefault());
        List<DiscussionThread> newThreads = discussionThreadRepository.findInActiveCoursesOfTutor(tutor.getId()).stream()
                .filter(thread -> !thread.getAuthor().getId().equals(tutor.getId()) && thread.getCreatedAt().isAfter(sinceLocal))
                .toList();
        items.addAll(discussion(discussionPostRepository.findStudentPostsForTutor(tutor.getId(), sinceLocal),
                newThreads, "/discussions/", "from students"));
        return items;
    }

    // ---- shared

    /**
     * One notification per thread: "2 new replies in "Arrays help"". No author names: classmates
     * can post anonymously, and the thread page applies those rules.
     */
    private List<Notification> discussion(List<DiscussionPost> posts, List<DiscussionThread> newThreads,
                                          String hrefPrefix, String whose) {
        Map<UUID, DiscussionThread> threads = new LinkedHashMap<>();
        Map<UUID, Integer> replies = new LinkedHashMap<>();
        Map<UUID, Instant> latest = new LinkedHashMap<>();
        for (DiscussionThread thread : newThreads) {
            threads.put(thread.getId(), thread);
            latest.merge(thread.getId(), instant(thread.getCreatedAt()), (a, b) -> a.isAfter(b) ? a : b);
        }
        for (DiscussionPost post : posts) {
            DiscussionThread thread = post.getThread();
            threads.putIfAbsent(thread.getId(), thread);
            replies.merge(thread.getId(), 1, Integer::sum);
            latest.merge(thread.getId(), instant(post.getCreatedAt()), (a, b) -> a.isAfter(b) ? a : b);
        }
        List<Notification> items = new ArrayList<>();
        for (DiscussionThread thread : threads.values()) {
            int count = replies.getOrDefault(thread.getId(), 0);
            boolean isNew = newThreads.contains(thread);
            String title = isNew && count == 0
                    ? "New post: " + thread.getTitle()
                    : (count == 1 ? "1 new reply" : count + " new replies") + " " + whose + ": " + thread.getTitle();
            Course course = thread.getCourse();
            items.add(new Notification("discussion-" + thread.getId(), NotificationKind.DISCUSSION, title, null,
                    course.getId(), course.getName(), hrefPrefix + thread.getId(), latest.get(thread.getId()), false));
        }
        return items;
    }

    private static boolean dueSoon(Instant dueAt, Instant now) {
        return dueAt.isAfter(now) && !dueAt.isAfter(now.plus(DUE_SOON));
    }

    /** The app's LocalDateTime columns are the JVM's local time. */
    private static Instant instant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static String preview(String text) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() > 140 ? flat.substring(0, 139) + "…" : flat;
    }

    private static Set<NotificationKind> muted(User user) {
        Set<NotificationKind> kinds = EnumSet.noneOf(NotificationKind.class);
        if (user.getMutedNotifications() == null || user.getMutedNotifications().isBlank()) {
            return kinds;
        }
        Arrays.stream(user.getMutedNotifications().split(","))
                .map(String::trim)
                .filter(name -> Arrays.stream(NotificationKind.values()).anyMatch(kind -> kind.name().equals(name)))
                .map(NotificationKind::valueOf)
                .forEach(kinds::add);
        return kinds;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
