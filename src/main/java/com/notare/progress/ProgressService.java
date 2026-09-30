package com.notare.progress;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentRepository;
import com.notare.progress.dto.CourseProgressResponse;
import com.notare.progress.dto.StudentProgressResponse;
import com.notare.progress.dto.StudentProgressResponse.ItemKind;
import com.notare.progress.dto.StudentProgressResponse.ItemStatus;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizRepository;
import com.notare.quizattempt.AttemptStatus;
import com.notare.quizattempt.QuizAttempt;
import com.notare.quizattempt.QuizAttemptRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Unit progress: how much of each unit's work (assignments, published quizzes) a student has done.
 * Units are the course's topics, in creation order, plus a trailing "No unit" bucket when some work
 * isn't in one. "Done" means submitted: feedback doesn't have to be released yet.
 */
@Service
@Transactional(readOnly = true)
public class ProgressService {

    static final String NO_UNIT = "No unit";

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TopicRepository topicRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

    public ProgressService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            TopicRepository topicRepository,
            AssignmentRepository assignmentRepository,
            QuizRepository quizRepository,
            SubmissionRepository submissionRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.topicRepository = topicRepository;
        this.assignmentRepository = assignmentRepository;
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
    }

    public StudentProgressResponse studentProgress(UUID courseId, String studentEmail) {
        User student = requireUser(studentEmail, UserRole.STUDENT);
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), candidate.getId()))
                // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        CourseWork work = loadWork(course);
        LocalDateTime now = LocalDateTime.now();

        Map<UUID, Submission> latestSubmission = new HashMap<>();
        for (Submission submission : submissionRepository.findByStudentId(student.getId())) {
            latestSubmission.merge(submission.getAssignment().getId(), submission,
                    (a, b) -> a.getAttemptNumber() >= b.getAttemptNumber() ? a : b);
        }
        Map<UUID, QuizAttempt> attempts = new HashMap<>();
        for (QuizAttempt attempt : quizAttemptRepository.findByStudentId(student.getId())) {
            attempts.put(attempt.getQuiz().getId(), attempt);
        }

        List<StudentProgressResponse.Unit> units = new ArrayList<>();
        for (UnitKey unit : work.units()) {
            List<StudentProgressResponse.Item> items = new ArrayList<>();
            for (Assignment assignment : work.assignmentsIn(unit)) {
                items.add(new StudentProgressResponse.Item(assignment.getId(), ItemKind.ASSIGNMENT,
                        assignment.getTitle(), assignment.getDueDate(), statusOf(latestSubmission.get(assignment.getId()))));
            }
            for (Quiz quiz : work.quizzesIn(unit)) {
                items.add(new StudentProgressResponse.Item(quiz.getId(), ItemKind.QUIZ,
                        quiz.getTitle(), null, statusOf(attempts.get(quiz.getId()), now)));
            }
            int done = (int) items.stream().filter(item -> isDone(item.status())).count();
            units.add(new StudentProgressResponse.Unit(unit.topicId(), unit.name(), items, done, items.size()));
        }
        return new StudentProgressResponse(units);
    }

    public CourseProgressResponse courseProgress(UUID courseId, String tutorEmail) {
        User tutor = requireUser(tutorEmail, UserRole.TUTOR);
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> candidate.getTutor().getId().equals(tutor.getId()))
                // 404, not 403 - avoid confirming another tutor's course exists
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        CourseWork work = loadWork(course);
        LocalDateTime now = LocalDateTime.now();

        // (studentId -> assignment/quiz IDs they've finished)
        Map<UUID, Set<UUID>> finished = new HashMap<>();
        for (Submission submission : submissionRepository.findByAssignment_Course_Id(course.getId())) {
            finished.computeIfAbsent(submission.getStudent().getId(), id -> new HashSet<>())
                    .add(submission.getAssignment().getId());
        }
        for (QuizAttempt attempt : quizAttemptRepository.findByQuiz_Course_Id(course.getId())) {
            if (isDone(statusOf(attempt, now))) {
                finished.computeIfAbsent(attempt.getStudent().getId(), id -> new HashSet<>())
                        .add(attempt.getQuiz().getId());
            }
        }

        List<CourseProgressResponse.UnitSummary> units = work.units().stream()
                .map(unit -> new CourseProgressResponse.UnitSummary(unit.topicId(), unit.name(),
                        work.assignmentsIn(unit).size(), work.quizzesIn(unit).size()))
                .toList();

        List<CourseProgressResponse.StudentRow> students = enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(student -> {
                    Set<UUID> done = finished.getOrDefault(student.getId(), Set.of());
                    List<Integer> perUnit = work.units().stream()
                            .map(unit -> (int) (work.assignmentsIn(unit).stream().filter(a -> done.contains(a.getId())).count()
                                    + work.quizzesIn(unit).stream().filter(q -> done.contains(q.getId())).count()))
                            .toList();
                    return new CourseProgressResponse.StudentRow(student.getId(), student.getName(), perUnit);
                })
                .toList();

        return new CourseProgressResponse(units, students);
    }

    private CourseWork loadWork(Course course) {
        List<Topic> topics = topicRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
        List<Assignment> assignments = assignmentRepository.findByCourseId(course.getId()).stream()
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .toList();
        List<Quiz> quizzes = quizRepository.findByCourseIdAndPublishedAtIsNotNull(course.getId()).stream()
                .sorted(Comparator.comparing(Quiz::getPublishedAt))
                .toList();

        List<UnitKey> units = new ArrayList<>(topics.stream().map(t -> new UnitKey(t.getId(), t.getName())).toList());
        boolean hasUngrouped = assignments.stream().anyMatch(a -> a.getTopic() == null)
                || quizzes.stream().anyMatch(q -> q.getTopic() == null);
        if (hasUngrouped) {
            units.add(new UnitKey(null, NO_UNIT));
        }
        return new CourseWork(units, assignments, quizzes);
    }

    private static ItemStatus statusOf(Submission latest) {
        if (latest == null) {
            return ItemStatus.NOT_STARTED;
        }
        return latest.getReleasedAt() != null ? ItemStatus.RETURNED : ItemStatus.SUBMITTED;
    }

    /** An attempt still IN_PROGRESS past its deadline is finished; it's only finalized lazily elsewhere. */
    private static ItemStatus statusOf(QuizAttempt attempt, LocalDateTime now) {
        if (attempt == null) {
            return ItemStatus.NOT_STARTED;
        }
        if (attempt.getReleasedAt() != null) {
            return ItemStatus.RETURNED;
        }
        boolean finished = attempt.getStatus() == AttemptStatus.SUBMITTED
                || (attempt.getDeadlineAt() != null && attempt.getDeadlineAt().isBefore(now));
        return finished ? ItemStatus.SUBMITTED : ItemStatus.IN_PROGRESS;
    }

    private static boolean isDone(ItemStatus status) {
        return status == ItemStatus.SUBMITTED || status == ItemStatus.RETURNED;
    }

    private User requireUser(String email, UserRole role) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private record UnitKey(UUID topicId, String name) {
    }

    private record CourseWork(List<UnitKey> units, List<Assignment> assignments, List<Quiz> quizzes) {
        List<Assignment> assignmentsIn(UnitKey unit) {
            return assignments.stream()
                    .filter(a -> Objects.equals(a.getTopic() != null ? a.getTopic().getId() : null, unit.topicId()))
                    .toList();
        }

        List<Quiz> quizzesIn(UnitKey unit) {
            return quizzes.stream()
                    .filter(q -> Objects.equals(q.getTopic() != null ? q.getTopic().getId() : null, unit.topicId()))
                    .toList();
        }
    }
}
