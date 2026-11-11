package com.notare.calendar;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentRepository;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizRepository;
import com.notare.session.Session;
import com.notare.session.SessionRepository;
import com.notare.session.SessionStatus;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calendar subscribe links. Each person gets a secret token; anyone holding the link can read
 * that person's schedule (class meetings, due times, tutoring sessions), so it's random, 256 bits,
 * and can be reset. The frontend serves the feed through a cached route so calendar apps polling
 * it don't keep waking the backend.
 */
@Service
public class CalendarFeedService {

    /** Weekly meetings are written out as single events within this window. */
    static final int MEETINGS_PAST_DAYS = 28;
    static final int MEETINGS_FUTURE_DAYS = 182;
    /** Older due times and sessions are left out to keep the feed small. */
    static final int HISTORY_DAYS = 90;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final SessionRepository sessionRepository;
    private final String appBaseUrl;

    public CalendarFeedService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            QuizRepository quizRepository,
            SessionRepository sessionRepository,
            @Value("${email.app-base-url}") String appBaseUrl
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.quizRepository = quizRepository;
        this.sessionRepository = sessionRepository;
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    // ---- the signed-in person's link

    @Transactional(readOnly = true)
    public String currentToken(String email) {
        return requireUser(email).getCalendarToken();
    }

    /** Creates the link, or replaces it so the old one stops working. */
    @Transactional
    public String resetToken(String email) {
        User user = requireUser(email);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        user.setCalendarToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        userRepository.save(user);
        return user.getCalendarToken();
    }

    // ---- the feed

    @Transactional(readOnly = true)
    public String feed(String token) {
        User user = (token == null || token.isBlank() ? null : userRepository.findByCalendarToken(token).orElse(null));
        if (user == null || !user.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar not found");
        }
        boolean tutor = user.getRole() == UserRole.TUTOR;
        Instant now = Instant.now();
        Instant since = now.minus(java.time.Duration.ofDays(HISTORY_DAYS));

        List<Course> courses = tutor
                ? courseRepository.findByTutorIdAndArchivedAtIsNull(user.getId())
                : enrollmentRepository.findByStudentId(user.getId()).stream()
                        .map(Enrollment::getCourse)
                        .filter(course -> course.getArchivedAt() == null)
                        .toList();
        courses = courses.stream().sorted(Comparator.comparing(Course::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        Map<UUID, Course> byId = courses.stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Set<UUID> courseIds = byId.keySet();

        ICalendar calendar = new ICalendar("Notare", "Your classes, due dates and tutoring sessions from Notare.", now);
        String prefix = tutor ? "" : "/student";

        for (Course course : courses) {
            writeMeetings(calendar, course, prefix);
        }

        if (!courseIds.isEmpty()) {
            for (Assignment assignment : assignmentRepository.findByCourseIdIn(courseIds)) {
                if (assignment.getDueAt().isBefore(since) || (!tutor && !assignment.isVisibleToStudents(now))) {
                    continue;
                }
                Course course = byId.get(assignment.getCourse().getId());
                calendar.event("assignment-" + assignment.getId() + "@notare", assignment.getDueAt(), null,
                        "Due: " + assignment.getTitle(), course.getName(),
                        appBaseUrl + prefix + "/assignments/" + assignment.getId());
            }
            for (Quiz quiz : quizRepository.findByCourseIdInAndPublishedAtIsNotNull(courseIds)) {
                if (quiz.getDueAt() == null || quiz.getDueAt().isBefore(since)) {
                    continue;
                }
                Course course = byId.get(quiz.getCourse().getId());
                calendar.event("quiz-" + quiz.getId() + "@notare", quiz.getDueAt(), null,
                        "Quiz due: " + quiz.getTitle(), course.getName(),
                        appBaseUrl + prefix + "/quizzes/" + quiz.getId());
            }
        }

        List<Session> sessions = tutor ? sessionRepository.findByTutorId(user.getId()) : sessionRepository.findByStudentId(user.getId());
        for (Session session : sessions) {
            if (session.getStatus() == SessionStatus.CANCELLED) {
                continue;
            }
            // Session times are a wall clock with no zone: read them in the course's zone, or the
            // default course zone for a session outside any course.
            ZoneId zone = session.getCourse() != null ? session.getCourse().zone() : ZoneId.of(Course.DEFAULT_TIME_ZONE);
            Instant start = session.getDate().atZone(zone).toInstant();
            if (start.isBefore(since)) {
                continue;
            }
            String with = tutor ? session.getStudent().getName() : session.getTutor().getName();
            calendar.event("session-" + session.getId() + "@notare", start,
                    start.plus(java.time.Duration.ofMinutes(session.getDuration())),
                    "Tutoring: " + session.getSubject(), "With " + with,
                    tutor ? appBaseUrl + "/sessions/" + session.getId() : null);
        }
        return calendar.build();
    }

    /** One event per class meeting within the window and the course's term. */
    private void writeMeetings(ICalendar calendar, Course course, String prefix) {
        Set<DayOfWeek> days = course.getMeetingDays();
        if (days == null || days.isEmpty() || course.getMeetingStartTime() == null || course.getMeetingEndTime() == null) {
            return;
        }
        ZoneId zone = course.zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate from = today.minusDays(MEETINGS_PAST_DAYS);
        LocalDate to = today.plusDays(MEETINGS_FUTURE_DAYS);
        if (course.getTermStartDate() != null && course.getTermStartDate().isAfter(from)) {
            from = course.getTermStartDate();
        }
        if (course.getTermEndDate() != null && course.getTermEndDate().isBefore(to)) {
            to = course.getTermEndDate();
        }
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!days.contains(day.getDayOfWeek())) {
                continue;
            }
            Instant start = day.atTime(course.getMeetingStartTime()).atZone(zone).toInstant();
            Instant end = day.atTime(course.getMeetingEndTime()).atZone(zone).toInstant();
            calendar.event("meeting-" + course.getId() + "-" + DAY.format(day) + "@notare", start, end,
                    course.getName(), course.getSubject(), appBaseUrl + prefix + "/courses/" + course.getId());
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
