package com.notare.home;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentRepository;
import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionThreadRead;
import com.notare.discussion.DiscussionThreadReadRepository;
import com.notare.discussion.DiscussionThreadRepository;
import com.notare.home.dto.HomeCourse;
import com.notare.home.dto.HomeThread;
import com.notare.home.dto.StudentHomeResponse;
import com.notare.home.dto.TutorHomeResponse;
import com.notare.quiz.QuestionType;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizRepository;
import com.notare.quizattempt.AttemptStatus;
import com.notare.quizattempt.QuizAnswerRepository;
import com.notare.quizattempt.QuizAttempt;
import com.notare.quizattempt.QuizAttemptRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only aggregates for the two home pages, so each loads in one request instead of one per
 * course. Only reads; every item links to an existing page that does its own authorization.
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

    // Student "Next up" window: overdue work stays visible for two weeks, upcoming work for a month.
    static final int OVERDUE_DAYS = 14;
    static final int UPCOMING_DAYS = 30;
    static final int RETURNED_DAYS = 14;

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final DiscussionThreadRepository threadRepository;
    private final DiscussionThreadReadRepository threadReadRepository;

    public HomeService(
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            DiscussionThreadRepository threadRepository,
            DiscussionThreadReadRepository threadReadRepository
    ) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.threadRepository = threadRepository;
        this.threadReadRepository = threadReadRepository;
    }

    public TutorHomeResponse tutorHome(String tutorEmail) {
        User tutor = requireUser(tutorEmail, UserRole.TUTOR);

        List<TutorHomeResponse.ReviewSubmission> submissions = submissionRepository.findUnreleasedForTutor(tutor.getId())
                .stream()
                .map(s -> new TutorHomeResponse.ReviewSubmission(
                        s.getId(),
                        s.getAssignment().getId(),
                        s.getAssignment().getTitle(),
                        HomeCourse.from(s.getAssignment().getCourse()),
                        s.getStudent().getName(),
                        s.getSubmittedAt(),
                        s.getSageFeedback() != null))
                .toList();

        List<TutorHomeResponse.ReviewQuizAttempt> attempts = quizAttemptRepository
                .findUnreleasedFinishedForTutor(tutor.getId(), LocalDateTime.now())
                .stream()
                .map(a -> new TutorHomeResponse.ReviewQuizAttempt(
                        a.getId(),
                        a.getQuiz().getId(),
                        a.getQuiz().getTitle(),
                        HomeCourse.from(a.getQuiz().getCourse()),
                        a.getStudent().getName(),
                        a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getDeadlineAt(),
                        needsGrading(a)))
                .toList();

        List<HomeThread> unread = unread(threadRepository.findInActiveCoursesOfTutor(tutor.getId()), tutor);

        return new TutorHomeResponse(submissions, attempts, unread);
    }

    public StudentHomeResponse studentHome(String studentEmail) {
        User student = requireUser(studentEmail, UserRole.STUDENT);
        Map<UUID, Course> courses = enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(Enrollment::getCourse)
                .filter(course -> course.getArchivedAt() == null)
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        if (courses.isEmpty()) {
            return new StudentHomeResponse(List.of(), List.of(), List.of(), List.of());
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<Submission> mySubmissions = submissionRepository.findByStudentId(student.getId());
        Set<UUID> submittedAssignmentIds = mySubmissions.stream()
                .map(s -> s.getAssignment().getId())
                .collect(Collectors.toSet());

        List<StudentHomeResponse.ToDoAssignment> toDo = assignmentRepository.findByCourseIdIn(courses.keySet()).stream()
                .filter(a -> !submittedAssignmentIds.contains(a.getId()))
                .filter(a -> !a.getDueDate().isBefore(today.minusDays(OVERDUE_DAYS))
                        && !a.getDueDate().isAfter(today.plusDays(UPCOMING_DAYS)))
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .map(a -> new StudentHomeResponse.ToDoAssignment(
                        a.getId(), a.getTitle(), HomeCourse.from(a.getCourse()), a.getDueDate()))
                .toList();

        Map<UUID, QuizAttempt> myAttemptsByQuiz = quizAttemptRepository.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(a -> a.getQuiz().getId(), Function.identity()));

        List<StudentHomeResponse.OpenQuiz> openQuizzes = quizRepository.findByCourseIdInAndPublishedAtIsNotNull(courses.keySet())
                .stream()
                .filter(quiz -> !isFinished(myAttemptsByQuiz.get(quiz.getId()), now))
                .sorted(Comparator.comparing(Quiz::getPublishedAt))
                .map(quiz -> {
                    QuizAttempt attempt = myAttemptsByQuiz.get(quiz.getId());
                    return new StudentHomeResponse.OpenQuiz(
                            quiz.getId(),
                            quiz.getTitle(),
                            HomeCourse.from(quiz.getCourse()),
                            quiz.getTimeLimitMinutes(),
                            attempt != null ? attempt.getId() : null,
                            attempt != null ? attempt.getDeadlineAt() : null);
                })
                .toList();

        LocalDateTime returnedSince = now.minusDays(RETURNED_DAYS);
        List<StudentHomeResponse.ReturnedWork> returned = new ArrayList<>();
        mySubmissions.stream()
                .filter(s -> s.getReleasedAt() != null && s.getReleasedAt().isAfter(returnedSince))
                .filter(s -> courses.containsKey(s.getAssignment().getCourse().getId()))
                .forEach(s -> returned.add(new StudentHomeResponse.ReturnedWork(
                        StudentHomeResponse.ReturnedWork.Kind.ASSIGNMENT,
                        s.getAssignment().getId(),
                        s.getAssignment().getTitle(),
                        HomeCourse.from(s.getAssignment().getCourse()),
                        s.getGrade(),
                        s.getReleasedAt())));
        myAttemptsByQuiz.values().stream()
                .filter(a -> a.getReleasedAt() != null && a.getReleasedAt().isAfter(returnedSince))
                .filter(a -> courses.containsKey(a.getQuiz().getCourse().getId()))
                .forEach(a -> returned.add(new StudentHomeResponse.ReturnedWork(
                        StudentHomeResponse.ReturnedWork.Kind.QUIZ,
                        a.getQuiz().getId(),
                        a.getQuiz().getTitle(),
                        HomeCourse.from(a.getQuiz().getCourse()),
                        null,
                        a.getReleasedAt())));
        returned.sort(Comparator.comparing(StudentHomeResponse.ReturnedWork::releasedAt).reversed());

        List<HomeThread> unread = unread(
                threadRepository.findVisibleToStudentInCourses(courses.keySet(), student.getId()), student);

        return new StudentHomeResponse(toDo, openQuizzes, List.copyOf(returned), unread);
    }

    /** Submitted, or past its deadline (finalized lazily elsewhere, so it may still say IN_PROGRESS). */
    private static boolean isFinished(QuizAttempt attempt, LocalDateTime now) {
        if (attempt == null) {
            return false;
        }
        return attempt.getStatus() == AttemptStatus.SUBMITTED
                || (attempt.getDeadlineAt() != null && attempt.getDeadlineAt().isBefore(now));
    }

    private boolean needsGrading(QuizAttempt attempt) {
        return quizAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .anyMatch(answer -> {
                    QuestionType type = answer.getQuestion().getType();
                    return (type == QuestionType.SHORT_ANSWER || type == QuestionType.ESSAY)
                            && answer.getPointsAwarded() == null;
                });
    }

    /** Same unread rule as the discussion list: never opened, or new activity since last opened. */
    private List<HomeThread> unread(List<DiscussionThread> threads, User viewer) {
        if (threads.isEmpty()) {
            return List.of();
        }
        Map<UUID, LocalDateTime> lastReads = new HashMap<>();
        for (DiscussionThreadRead read : threadReadRepository.findByUserIdAndThreadIdIn(
                viewer.getId(), threads.stream().map(DiscussionThread::getId).toList())) {
            lastReads.put(read.getThread().getId(), read.getLastReadAt());
        }
        return threads.stream()
                .filter(thread -> {
                    LocalDateTime lastRead = lastReads.get(thread.getId());
                    return lastRead == null || thread.getLastActivityAt().isAfter(lastRead);
                })
                .map(HomeThread::from)
                .toList();
    }

    private User requireUser(String email, UserRole role) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
