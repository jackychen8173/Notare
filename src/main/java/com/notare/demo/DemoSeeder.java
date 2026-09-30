package com.notare.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notare.announcement.Announcement;
import com.notare.announcement.AnnouncementRepository;
import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.CourseColor;
import com.notare.course.CourseRepository;
import com.notare.course.CourseService;
import com.notare.course.Enrollment;
import com.notare.course.EnrollmentId;
import com.notare.course.EnrollmentRepository;
import com.notare.discussion.DiscussionPost;
import com.notare.discussion.DiscussionPostRepository;
import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionThreadRepository;
import com.notare.discussion.DiscussionVisibility;
import com.notare.material.Material;
import com.notare.material.MaterialRepository;
import com.notare.material.MaterialType;
import com.notare.quiz.QuestionType;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionOption;
import com.notare.quiz.QuizQuestionOptionRepository;
import com.notare.quiz.QuizQuestionRepository;
import com.notare.quiz.QuizRepository;
import com.notare.sage.SageFeedback;
import com.notare.session.Session;
import com.notare.session.SessionNote;
import com.notare.session.SessionNoteRepository;
import com.notare.session.SessionRepository;
import com.notare.session.SessionStatus;
import com.notare.submission.FeedbackStatus;
import com.notare.submission.Submission;
import com.notare.submission.LineCommentSource;
import com.notare.submission.LineCommentStatus;
import com.notare.submission.SubmissionLineComment;
import com.notare.submission.SubmissionLineCommentRepository;
import com.notare.submission.SubmissionRepository;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Builds one private, self-contained demo classroom: a teacher, the student the visitor may log in
 * as, three classmates, two courses (with meeting schedules), and a spread of sample content that
 * exercises every major screen. Dates are relative to today so the dashboard, "due soon" list and
 * calendar always look alive. Writes entities directly rather than through the services, because
 * the interesting states (a released submission with Sage feedback, a Sage draft waiting for review,
 * a completed session with notes) come from flows the demo can't run (Sage is off in the demo).
 */
@Component
class DemoSeeder {

    record DemoAccounts(User tutor, User student) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final TopicRepository topicRepository;
    private final MaterialRepository materialRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionLineCommentRepository lineCommentRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final AnnouncementRepository announcementRepository;
    private final DiscussionThreadRepository discussionThreadRepository;
    private final DiscussionPostRepository discussionPostRepository;
    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final ObjectMapper objectMapper;

    DemoSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CourseRepository courseRepository,
            CourseService courseService,
            EnrollmentRepository enrollmentRepository,
            TopicRepository topicRepository,
            MaterialRepository materialRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            SubmissionLineCommentRepository lineCommentRepository,
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizQuestionOptionRepository quizQuestionOptionRepository,
            AnnouncementRepository announcementRepository,
            DiscussionThreadRepository discussionThreadRepository,
            DiscussionPostRepository discussionPostRepository,
            SessionRepository sessionRepository,
            SessionNoteRepository sessionNoteRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.topicRepository = topicRepository;
        this.materialRepository = materialRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.lineCommentRepository = lineCommentRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizQuestionOptionRepository = quizQuestionOptionRepository;
        this.announcementRepository = announcementRepository;
        this.discussionThreadRepository = discussionThreadRepository;
        this.discussionPostRepository = discussionPostRepository;
        this.sessionRepository = sessionRepository;
        this.sessionNoteRepository = sessionNoteRepository;
        this.objectMapper = objectMapper;
    }

    DemoAccounts seed() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        User tutor = demoUser("Alex Rivera", UserRole.TUTOR);
        User student = demoUser("Jordan Lee", UserRole.STUDENT);
        User sam = demoUser("Sam Patel", UserRole.STUDENT);
        User maya = demoUser("Maya Chen", UserRole.STUDENT);
        User diego = demoUser("Diego Alvarez", UserRole.STUDENT);

        Course period3 = course(tutor, "AP Computer Science A: Period 3", CourseColor.INDIGO,
                "Java fundamentals through object-oriented programming, following the AP CSA units.",
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0), LocalTime.of(9, 50), today.minusDays(35), today.plusDays(240));
        Course period5 = course(tutor, "AP Computer Science A: Period 5", CourseColor.ROSE,
                "Second section of AP CSA, same pacing as Period 3.",
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                LocalTime.of(13, 0), LocalTime.of(14, 20), today.minusDays(35), today.plusDays(240));

        enroll(student, period3);
        enroll(sam, period3);
        enroll(maya, period3);
        enroll(diego, period5);
        enroll(sam, period5);

        Topic unit1 = topic(period3, "Unit 1: Primitive Types");
        Topic unit2 = topic(period3, "Unit 2: Using Objects");
        Topic unit3 = topic(period3, "Unit 3: Boolean Expressions and if Statements");

        materialRepository.save(Material.builder()
                .course(period3).topic(unit1)
                .title("Java tutorial: Variables and primitive types")
                .description("Read before Wednesday's class.")
                .url("https://docs.oracle.com/javase/tutorial/java/nutsandbolts/variables.html")
                .type(MaterialType.LINK)
                .build());

        Assignment converter = assignment(period3, unit1, "Temperature Converter",
                "Write a class `TemperatureConverter` whose `main` method converts 98.6°F to Celsius and prints the result "
                        + "rounded to one decimal place. Use the formula C = (F - 32) * 5 / 9 and watch out for integer division.",
                today.minusDays(7));
        // Resubmission is on here, so the demo student can revise after reading the line comments.
        converter.setAllowResubmission(true);
        assignmentRepository.save(converter);
        Assignment strings = assignment(period3, unit2, "String Methods Practice",
                "Given a full name like \"Ada Lovelace\", print the initials, the name in all caps, and the number of "
                        + "characters (not counting the space) using `substring`, `indexOf`, `toUpperCase` and `length`.",
                today.plusDays(3));
        assignment(period3, unit3, "Grade Calculator",
                "Read a numeric score and print the letter grade using an if / else if chain. Handle scores above 100 "
                        + "and below 0 with an error message.",
                today.plusDays(8));
        assignment(period3, unit3, "Unit 3 Project: Number Guessing Logic",
                "Write the logic for a number guessing game: given a secret number and a guess, print \"Too high\", "
                        + "\"Too low\" or \"Correct!\". Bonus: count the guesses.",
                today.plusDays(14));
        Assignment converterP5 = assignment(period5, null, "Temperature Converter",
                "Write a class `TemperatureConverter` whose `main` method converts 98.6°F to Celsius and prints the result.",
                today.minusDays(5));

        // Released: what the demo student sees once the teacher approves Sage's feedback.
        Submission released = submissionRepository.save(Submission.builder()
                .assignment(converter).student(student)
                .content(CONVERTER_GOOD)
                .sageFeedback(json(new SageFeedback(
                        "Correct: the conversion uses 5.0 / 9 so it avoids integer division, and the output matches the expected 37.0.",
                        "Clear variable names. Consider making the Fahrenheit value a named constant.",
                        "Try rounding with Math.round(celsius * 10) / 10.0 instead of printf, so the value itself is rounded.",
                        "Nice work spotting the integer-division trap; that's the most common mistake on this one.")))
                .tutorFeedback("Great job, Jordan. You caught the integer division issue. Next time, try the Math.round approach Sage mentioned.")
                .feedbackStatus(FeedbackStatus.REVISED)
                .grade("A-")
                .submittedAt(now.minusDays(8))
                .releasedAt(now.minusDays(6))
                .build());
        lineComment(released, 4, LineCommentSource.TUTOR, LineCommentStatus.PUBLISHED,
                "Good call writing 5.0 here. With 5 / 9 this line would quietly compute 0.");
        lineComment(released, 5, LineCommentSource.SAGE, LineCommentStatus.PUBLISHED,
                "printf rounds only what's printed. The celsius variable still holds 37.0000...");
        // Waiting on the teacher: a Sage draft is ready to review, edit and release.
        Submission samDraft = submissionRepository.save(Submission.builder()
                .assignment(converter).student(sam)
                .content(CONVERTER_INT_DIVISION)
                .sageFeedback(json(new SageFeedback(
                        "Not quite: (f - 32) * 5 / 9 is evaluated with int arithmetic, so the result is 37 instead of 37.0 for 98.6°F, and it truncates for other inputs.",
                        "Readable and well indented.",
                        "Declare the variables as double, or divide by 9.0, so Java uses floating-point division.",
                        "You're very close. This is a one-character fix.")))
                .feedbackStatus(FeedbackStatus.PENDING)
                .submittedAt(now.minusDays(7).minusHours(3))
                .build());
        // Sage's line suggestions: only the teacher sees these until they accept one.
        lineComment(samDraft, 3, LineCommentSource.SAGE, LineCommentStatus.SUGGESTED,
                "An int can't hold 98.6, so the input is already off before any math happens. Try double.");
        lineComment(samDraft, 4, LineCommentSource.SAGE, LineCommentStatus.SUGGESTED,
                "5 / 9 with ints is integer division. Use 5.0 / 9 so Java keeps the decimal part.");
        submissionRepository.save(Submission.builder()
                .assignment(converter).student(maya)
                .content(CONVERTER_GOOD.replace("celsius", "c"))
                .feedbackStatus(FeedbackStatus.PENDING)
                .submittedAt(now.minusDays(7).minusHours(1))
                .build());
        submissionRepository.save(Submission.builder()
                .assignment(strings).student(maya)
                .content(STRINGS)
                .feedbackStatus(FeedbackStatus.PENDING)
                .submittedAt(now.minusHours(20))
                .build());
        submissionRepository.save(Submission.builder()
                .assignment(converterP5).student(diego)
                .content(CONVERTER_GOOD)
                .feedbackStatus(FeedbackStatus.PENDING)
                .submittedAt(now.minusDays(5).minusHours(2))
                .build());

        Quiz checkIn = quizRepository.save(Quiz.builder()
                .course(period3).topic(unit1)
                .title("Unit 1 Check-in")
                .description("Three quick questions on primitive types. Multiple choice and true/false are graded instantly.")
                .timeLimitMinutes(10)
                .publishedAt(now.minusDays(2))
                .build());
        QuizQuestion division = question(checkIn, QuestionType.MULTIPLE_CHOICE,
                "What does `System.out.println(7 / 2);` print?", 1, null, 0);
        option(division, "3.5", false, 0);
        option(division, "3", true, 1);
        option(division, "4", false, 2);
        option(division, "3.0", false, 3);
        QuizQuestion doubles = question(checkIn, QuestionType.TRUE_FALSE,
                "A `double` variable can store the value 3.14.", 1, null, 1);
        option(doubles, "True", true, 0);
        option(doubles, "False", false, 1);
        question(checkIn, QuestionType.SHORT_ANSWER,
                "In one or two sentences, explain when you would use an `int` instead of a `double`.", 2,
                "Use int for whole-number counts (loop counters, number of items); double for measurements with fractions.", 2);

        Quiz draft = quizRepository.save(Quiz.builder()
                .course(period3).topic(unit2)
                .title("Unit 2 Quiz")
                .description("Draft: not visible to students until published.")
                .build());
        QuizQuestion substring = question(draft, QuestionType.MULTIPLE_CHOICE,
                "What does `\"computer\".substring(3, 6)` return?", 1, null, 0);
        option(substring, "\"put\"", true, 0);
        option(substring, "\"pute\"", false, 1);
        option(substring, "\"mpu\"", false, 2);

        announcementRepository.save(Announcement.builder()
                .course(period3).tutor(tutor)
                .content("Welcome to AP CSA! The Unit 1 Check-in quiz is open; it takes about 10 minutes. "
                        + "Office hours are Tuesdays at 3:30 in room 214.")
                .createdAt(now.minusDays(2))
                .build());
        announcementRepository.save(Announcement.builder()
                .course(period3).tutor(tutor)
                .content("Reminder: String Methods Practice is due this week. Bring questions to Wednesday's class.")
                .createdAt(now.minusHours(5))
                .build());

        DiscussionThread divisionThread = discussionThreadRepository.save(DiscussionThread.builder()
                .course(period3).author(maya)
                .visibility(DiscussionVisibility.PUBLIC)
                .title("Why does 7 / 2 give 3?")
                .body("I expected 3.5. Is my computer broken?")
                .anonymous(true)
                .createdAt(now.minusDays(3))
                .lastActivityAt(now.minusDays(2))
                .build());
        discussionPostRepository.save(DiscussionPost.builder()
                .thread(divisionThread).author(tutor)
                .body("Great question! When both operands are ints, Java does integer division and drops the remainder. "
                        + "Write 7 / 2.0 (or cast one side to double) to get 3.5.")
                .anonymous(false)
                .createdAt(now.minusDays(2))
                .build());
        discussionThreadRepository.save(DiscussionThread.builder()
                .course(period3).author(tutor)
                .visibility(DiscussionVisibility.PUBLIC)
                .title("Study group for the Unit 1 quiz?")
                .body("Post here if you'd like to form a study group. I'll share practice problems.")
                .anonymous(false)
                .pinned(true)
                .createdAt(now.minusDays(4))
                .build());
        DiscussionThread privateThread = discussionThreadRepository.save(DiscussionThread.builder()
                .course(period3).author(student)
                .visibility(DiscussionVisibility.PRIVATE)
                .title("Extra practice on if statements?")
                .body("Could you recommend some extra practice before the Grade Calculator assignment?")
                .anonymous(false)
                .createdAt(now.minusDays(1))
                .lastActivityAt(now.minusHours(18))
                .build());
        discussionPostRepository.save(DiscussionPost.builder()
                .thread(privateThread).author(tutor)
                .body("Sure! Try CodingBat's Logic-1 set, and we can go over it in Thursday's session.")
                .anonymous(false)
                .createdAt(now.minusHours(18))
                .build());

        Session pastSession = sessionRepository.save(Session.builder()
                .tutor(tutor).student(student).course(period3)
                .date(today.minusDays(3).atTime(16, 0))
                .subject("Integer division and casting")
                .duration(45)
                .status(SessionStatus.COMPLETED)
                .build());
        sessionNoteRepository.save(SessionNote.builder()
                .session(pastSession)
                .rawNotes("went over int vs double division. jordan got casting after 2 examples. "
                        + "still mixing up (double)(a/b) vs (double)a/b. hw: 3 practice problems")
                .formattedNotes("Summary\nWe reviewed integer vs floating-point division and casting.\n\n"
                        + "Progress\nJordan understood casting after two worked examples.\n\n"
                        + "Still working on\nThe difference between (double)(a / b) and (double) a / b.\n\n"
                        + "Next steps\nThree practice problems on casting before the next session.")
                .build());
        sessionRepository.save(Session.builder()
                .tutor(tutor).student(student).course(period3)
                .date(today.plusDays(2).atTime(16, 0))
                .subject("if statements and the Grade Calculator")
                .duration(45)
                .status(SessionStatus.SCHEDULED)
                .build());
        sessionRepository.save(Session.builder()
                .tutor(tutor).student(sam).course(period3)
                .date(today.plusDays(1).atTime(15, 30))
                .subject("Fixing integer division")
                .duration(30)
                .status(SessionStatus.SCHEDULED)
                .build());

        return new DemoAccounts(tutor, student);
    }

    private User demoUser(String name, UserRole role) {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        return userRepository.save(User.builder()
                .name(name)
                .email("demo-" + role.name().toLowerCase() + "-" + suffix + "@demo.notare.app")
                // Never used: demo accounts are only reachable through the one-click demo login.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role)
                .demo(true)
                .build());
    }

    private Course course(User tutor, String name, CourseColor color, String description, EnumSet<DayOfWeek> days,
                          LocalTime start, LocalTime end, LocalDate termStart, LocalDate termEnd) {
        return courseRepository.save(Course.builder()
                .tutor(tutor)
                .name(name)
                .subject("Computer Science")
                .description(description)
                .joinCode(courseService.generateUniqueJoinCode())
                .color(color)
                .meetingDays(days)
                .meetingStartTime(start)
                .meetingEndTime(end)
                .termStartDate(termStart)
                .termEndDate(termEnd)
                .build());
    }

    private void enroll(User student, Course course) {
        enrollmentRepository.save(Enrollment.builder()
                .id(new EnrollmentId(student.getId(), course.getId()))
                .student(student)
                .course(course)
                .build());
    }

    private void lineComment(Submission submission, int line, LineCommentSource source, LineCommentStatus status,
                             String body) {
        lineCommentRepository.save(SubmissionLineComment.builder()
                .submission(submission).lineNumber(line).source(source).status(status).body(body)
                .build());
    }

    private Topic topic(Course course, String name) {
        return topicRepository.save(Topic.builder().course(course).name(name).build());
    }

    private Assignment assignment(Course course, Topic topic, String title, String description, LocalDate dueDate) {
        return assignmentRepository.save(Assignment.builder()
                .course(course).topic(topic).title(title).description(description).dueDate(dueDate)
                .build());
    }

    private QuizQuestion question(Quiz quiz, QuestionType type, String prompt, int points, String referenceAnswer,
                                  int position) {
        return quizQuestionRepository.save(QuizQuestion.builder()
                .quiz(quiz).type(type).prompt(prompt)
                .pointsPossible(BigDecimal.valueOf(points))
                .referenceAnswer(referenceAnswer)
                .position(position)
                .build());
    }

    private void option(QuizQuestion question, String text, boolean correct, int position) {
        quizQuestionOptionRepository.save(QuizQuestionOption.builder()
                .question(question).text(text).correct(correct).position(position)
                .build());
    }

    private String json(SageFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize demo Sage feedback", e);
        }
    }

    private static final String CONVERTER_GOOD = """
            public class TemperatureConverter {
                public static void main(String[] args) {
                    double fahrenheit = 98.6;
                    double celsius = (fahrenheit - 32) * 5.0 / 9;
                    System.out.printf("%.1f°F is %.1f°C%n", fahrenheit, celsius);
                }
            }
            """;

    private static final String CONVERTER_INT_DIVISION = """
            public class TemperatureConverter {
                public static void main(String[] args) {
                    int f = 98;
                    int c = (f - 32) * 5 / 9;
                    System.out.println(f + "F is " + c + "C");
                }
            }
            """;

    private static final String STRINGS = """
            public class NameInfo {
                public static void main(String[] args) {
                    String name = "Ada Lovelace";
                    int space = name.indexOf(" ");
                    String initials = name.substring(0, 1) + name.substring(space + 1, space + 2);
                    System.out.println(initials);
                    System.out.println(name.toUpperCase());
                    System.out.println(name.length() - 1);
                }
            }
            """;
}
