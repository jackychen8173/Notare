package com.notare.gradebook;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.Lateness;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentRepository;
import com.notare.gradebook.dto.GradebookResponse;
import com.notare.gradebook.dto.GradebookResponse.Category;
import com.notare.gradebook.dto.GradebookResponse.Cell;
import com.notare.gradebook.dto.GradebookResponse.CellStatus;
import com.notare.gradebook.dto.GradebookResponse.Column;
import com.notare.gradebook.dto.GradebookResponse.ItemKind;
import com.notare.gradebook.dto.GradebookResponse.StudentRow;
import com.notare.gradebook.dto.StudentGradesResponse;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionRepository;
import com.notare.quiz.QuizRepository;
import com.notare.quizattempt.AttemptStatus;
import com.notare.quizattempt.QuizAnswer;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The gradebook (tutor) and a student's own grades. Both are built from the same columns and cells
 * so the two can't disagree; the student view only drops what the student can't see (unopened
 * assignments, class averages, other students). Points come only from released work.
 */
@Service
@Transactional(readOnly = true)
public class GradebookService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeCategoryRepository gradeCategoryRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;

    public GradebookService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            GradeCategoryRepository gradeCategoryRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.userRepository = userRepository;
    }

    public GradebookResponse gradebook(UUID courseId, String tutorEmail) {
        User tutor = requireUser(tutorEmail, UserRole.TUTOR);
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> candidate.getTutor().getId().equals(tutor.getId()))
                // 404, not 403 - avoid confirming another tutor's course exists
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<User> students = enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        CourseItems items = loadItems(course, false);
        WorkIndex work = loadWork(course, null);
        Instant now = Instant.now();

        List<List<Cell>> cellsByStudent = students.stream()
                .map(student -> items.cellsFor(student.getId(), work, now))
                .toList();

        List<Column> columns = new ArrayList<>();
        for (int i = 0; i < items.columns().size(); i++) {
            int index = i;
            Column column = items.columns().get(i);
            List<BigDecimal> percents = cellsByStudent.stream()
                    .map(cells -> cells.get(index).points())
                    .filter(Objects::nonNull)
                    .map(points -> GradeMath.percent(GradeMath.ratio(points, column.pointsPossible())))
                    .filter(Objects::nonNull)
                    .toList();
            columns.add(withAverage(column, average(percents)));
        }

        List<StudentRow> rows = new ArrayList<>();
        for (int s = 0; s < students.size(); s++) {
            User student = students.get(s);
            List<Cell> cells = cellsByStudent.get(s);
            GradeMath.Result grade = GradeMath.grade(items.categories(), columns, cells);
            rows.add(new StudentRow(student.getId(), student.getName(), student.getEmail(), cells,
                    grade.categoryPercents(), grade.coursePercent()));
        }
        return new GradebookResponse(course.getTimeZone(), items.categories(), columns, rows);
    }

    public StudentGradesResponse myGrades(UUID courseId, String studentEmail) {
        User student = requireUser(studentEmail, UserRole.STUDENT);
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), candidate.getId()))
                // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        CourseItems items = loadItems(course, true);
        WorkIndex work = loadWork(course, student.getId());
        List<Cell> cells = items.cellsFor(student.getId(), work, Instant.now());
        GradeMath.Result grade = GradeMath.grade(items.categories(), items.columns(), cells);

        List<StudentGradesResponse.CategoryGrade> categories = new ArrayList<>();
        for (int c = 0; c < items.categories().size(); c++) {
            Category category = items.categories().get(c);
            categories.add(new StudentGradesResponse.CategoryGrade(category.id(), category.name(),
                    category.weightPercent(), grade.categoryPercents().get(c)));
        }
        List<StudentGradesResponse.Item> list = new ArrayList<>();
        for (int i = 0; i < items.columns().size(); i++) {
            list.add(new StudentGradesResponse.Item(items.columns().get(i), cells.get(i)));
        }
        return new StudentGradesResponse(course.getTimeZone(), categories, list, grade.coursePercent());
    }

    // ---- loading

    /** studentView leaves out assignments that haven't opened yet. */
    private CourseItems loadItems(Course course, boolean studentView) {
        ZoneId zone = course.zone();
        Instant now = Instant.now();
        List<Category> categories = gradeCategoryRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                .map(category -> new Category(category.getId(), category.getName(), category.getWeightPercent()))
                .toList();
        boolean hasCategories = !categories.isEmpty();

        List<Assignment> assignments = assignmentRepository.findByCourseId(course.getId()).stream()
                .filter(assignment -> !studentView || assignment.isVisibleToStudents(now))
                .toList();
        List<Quiz> quizzes = quizRepository.findByCourseIdAndPublishedAtIsNotNull(course.getId());

        Map<UUID, BigDecimal> quizTotals = new HashMap<>();
        for (QuizQuestion question : quizQuestionRepository.findByQuiz_Course_Id(course.getId())) {
            quizTotals.merge(question.getQuiz().getId(), question.getPointsPossible(), BigDecimal::add);
        }

        List<Item> list = new ArrayList<>();
        for (Assignment assignment : assignments) {
            BigDecimal possible = assignment.getPointsPossible() == null ? null : BigDecimal.valueOf(assignment.getPointsPossible());
            UUID categoryId = assignment.getGradeCategory() != null ? assignment.getGradeCategory().getId() : null;
            Column column = new Column(assignment.getId(), ItemKind.ASSIGNMENT, assignment.getTitle(),
                    inZone(assignment.getDueAt(), zone), inZone(assignment.getOpensAt(), zone), possible, categoryId,
                    GradeMath.countsTowardGrade(possible, categoryId, hasCategories), null);
            list.add(new Item(column, assignment, null, assignment.getDueAt()));
        }
        for (Quiz quiz : quizzes) {
            BigDecimal total = quizTotals.get(quiz.getId());
            BigDecimal possible = total != null && total.signum() > 0 ? total : null;
            UUID categoryId = quiz.getGradeCategory() != null ? quiz.getGradeCategory().getId() : null;
            Column column = new Column(quiz.getId(), ItemKind.QUIZ, quiz.getTitle(), null, null, possible, categoryId,
                    GradeMath.countsTowardGrade(possible, categoryId, hasCategories), null);
            list.add(new Item(column, null, quiz, quiz.getPublishedAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        // Oldest first: assignments by due time, quizzes by when they were published.
        list.sort(Comparator.comparing(Item::sortKey));
        return new CourseItems(categories, list);
    }

    /** onlyStudentId limits it to one student's work (the student view). */
    private WorkIndex loadWork(Course course, UUID onlyStudentId) {
        Map<Key, List<Submission>> submissions = new HashMap<>();
        for (Submission submission : submissionRepository.findByAssignment_Course_Id(course.getId())) {
            UUID studentId = submission.getStudent().getId();
            if (onlyStudentId != null && !onlyStudentId.equals(studentId)) {
                continue;
            }
            submissions.computeIfAbsent(new Key(studentId, submission.getAssignment().getId()), key -> new ArrayList<>())
                    .add(submission);
        }
        submissions.values().forEach(list -> list.sort(Comparator.comparingInt(Submission::getAttemptNumber).reversed()));

        Map<Key, QuizAttempt> attempts = new HashMap<>();
        for (QuizAttempt attempt : quizAttemptRepository.findByQuiz_Course_Id(course.getId())) {
            UUID studentId = attempt.getStudent().getId();
            if (onlyStudentId == null || onlyStudentId.equals(studentId)) {
                attempts.put(new Key(studentId, attempt.getQuiz().getId()), attempt);
            }
        }
        Map<UUID, BigDecimal> awarded = new HashMap<>();
        for (QuizAnswer answer : quizAnswerRepository.findByAttempt_Quiz_Course_Id(course.getId())) {
            if (answer.getPointsAwarded() != null) {
                awarded.merge(answer.getAttempt().getId(), answer.getPointsAwarded(), BigDecimal::add);
            }
        }
        return new WorkIndex(submissions, attempts, awarded);
    }

    // ---- cells

    private static Cell assignmentCell(Assignment assignment, BigDecimal pointsPossible, List<Submission> versions, Instant now) {
        if (versions == null || versions.isEmpty()) {
            CellStatus status = now.isAfter(assignment.getDueAt()) ? CellStatus.MISSING : CellStatus.NOT_SUBMITTED;
            return new Cell(status, null, null, null, false, 0, 0, null);
        }
        Submission latest = versions.get(0);
        Submission released = versions.stream().filter(version -> version.getReleasedAt() != null).findFirst().orElse(null);
        Submission source = released != null ? released : latest;
        Lateness lateness = Lateness.of(assignment, source.submittedInstant());

        BigDecimal before = released != null && pointsPossible != null ? released.getPointsEarned() : null;
        CellStatus status = latest.getReleasedAt() != null ? CellStatus.RETURNED : CellStatus.SUBMITTED;
        return new Cell(status, lateness.apply(before), before, released != null ? released.getGrade() : null,
                lateness.late(), lateness.daysLate(), lateness.penaltyPercent(), latest.getId());
    }

    private static Cell quizCell(BigDecimal pointsPossible, QuizAttempt attempt, Map<UUID, BigDecimal> awarded) {
        if (attempt == null) {
            return new Cell(CellStatus.NOT_SUBMITTED, null, null, null, false, 0, 0, null);
        }
        if (attempt.getReleasedAt() != null) {
            BigDecimal points = pointsPossible == null ? null : awarded.getOrDefault(attempt.getId(), BigDecimal.ZERO);
            return new Cell(CellStatus.RETURNED, points, points, null, false, 0, 0, attempt.getId());
        }
        // An attempt still IN_PROGRESS past its deadline is finished; it's only finalized lazily elsewhere.
        boolean finished = attempt.getStatus() == AttemptStatus.SUBMITTED
                || (attempt.getDeadlineAt() != null && attempt.getDeadlineAt().isBefore(LocalDateTime.now()));
        return new Cell(finished ? CellStatus.SUBMITTED : CellStatus.IN_PROGRESS, null, null, null, false, 0, 0, attempt.getId());
    }

    private static Column withAverage(Column column, BigDecimal averagePercent) {
        return new Column(column.id(), column.kind(), column.title(), column.dueAt(), column.opensAt(),
                column.pointsPossible(), column.categoryId(), column.countsTowardGrade(), averagePercent);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private static OffsetDateTime inZone(Instant instant, ZoneId zone) {
        return instant == null ? null : instant.atZone(zone).toOffsetDateTime();
    }

    private User requireUser(String email, UserRole role) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private record Key(UUID studentId, UUID itemId) {
    }

    private record Item(Column column, Assignment assignment, Quiz quiz, Instant sortKey) {
    }

    private record WorkIndex(Map<Key, List<Submission>> submissions, Map<Key, QuizAttempt> attempts, Map<UUID, BigDecimal> quizPoints) {
    }

    private record CourseItems(List<Category> categories, List<Item> items) {
        List<Column> columns() {
            return items.stream().map(Item::column).toList();
        }

        List<Cell> cellsFor(UUID studentId, WorkIndex work, Instant now) {
            return items.stream()
                    .map(item -> item.assignment() != null
                            ? assignmentCell(item.assignment(), item.column().pointsPossible(),
                                    work.submissions().get(new Key(studentId, item.assignment().getId())), now)
                            : quizCell(item.column().pointsPossible(),
                                    work.attempts().get(new Key(studentId, item.quiz().getId())), work.quizPoints()))
                    .toList();
        }
    }
}
