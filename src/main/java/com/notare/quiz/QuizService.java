package com.notare.quiz;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.gradecategory.GradeCategory;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.quiz.dto.CreateQuestionRequest;
import com.notare.quiz.dto.CreateQuizRequest;
import com.notare.quiz.dto.OptionResponse;
import com.notare.quiz.dto.QuestionResponse;
import com.notare.quiz.dto.QuizResponse;
import com.notare.quiz.dto.UpdateQuestionRequest;
import com.notare.quiz.dto.UpdateQuizRequest;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionOptionRepository optionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final GradeCategoryRepository gradeCategoryRepository;

    public QuizService(
            QuizRepository quizRepository,
            QuizQuestionRepository questionRepository,
            QuizQuestionOptionRepository optionRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            TopicRepository topicRepository,
            GradeCategoryRepository gradeCategoryRepository
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
    }

    // ---- tutor: quiz ----

    public QuizResponse createQuiz(UUID courseId, CreateQuizRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add quizzes to an archived course");
        }

        Topic topic = resolveTopic(request.topicId(), course);
        GradeCategory gradeCategory = resolveGradeCategory(request.gradeCategoryId(), course);

        Quiz quiz = Quiz.builder()
                .course(course)
                .topic(topic)
                .gradeCategory(gradeCategory)
                .title(request.title())
                .description(request.description())
                .timeLimitMinutes(request.timeLimitMinutes())
                .build();

        quizRepository.save(quiz);

        return loadQuizResponse(quiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listQuizzes(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return quizRepository.findByCourseId(course.getId()).stream()
                .map(this::loadQuizResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(UUID quizId, String tutorEmail) {
        return loadQuizResponse(requireOwnedQuiz(quizId, tutorEmail));
    }

    public QuizResponse updateQuiz(UUID quizId, UpdateQuizRequest request, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);

        Topic topic = resolveTopic(request.topicId(), quiz.getCourse());
        GradeCategory gradeCategory = resolveGradeCategory(request.gradeCategoryId(), quiz.getCourse());

        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        quiz.setTopic(topic);
        quiz.setGradeCategory(gradeCategory);
        quizRepository.save(quiz);

        return loadQuizResponse(quiz);
    }

    public QuizResponse publishQuiz(UUID quizId, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);

        if (questionRepository.countByQuizId(quiz.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot publish a quiz with no questions");
        }

        quiz.setPublishedAt(java.time.LocalDateTime.now());
        quizRepository.save(quiz);

        return loadQuizResponse(quiz);
    }

    public QuizResponse unpublishQuiz(UUID quizId, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);
        quiz.setPublishedAt(null);
        quizRepository.save(quiz);
        return loadQuizResponse(quiz);
    }

    // ---- tutor: questions ----

    public QuizResponse addQuestion(UUID quizId, CreateQuestionRequest request, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .type(request.type())
                .prompt(request.prompt())
                .pointsPossible(request.pointsPossible())
                .referenceAnswer(request.referenceAnswer())
                .position((int) questionRepository.countByQuizId(quiz.getId()))
                .build();
        questionRepository.save(question);

        applyOptions(question, request.type(), request.options(), request.correctBoolean());

        return loadQuizResponse(quiz);
    }

    public QuizResponse updateQuestion(UUID quizId, UUID questionId, UpdateQuestionRequest request, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);
        QuizQuestion question = requireOwnedQuestion(quiz, questionId);

        question.setType(request.type());
        question.setPrompt(request.prompt());
        question.setPointsPossible(request.pointsPossible());
        question.setReferenceAnswer(request.referenceAnswer());
        questionRepository.save(question);

        optionRepository.deleteByQuestionId(question.getId());
        applyOptions(question, request.type(), request.options(), request.correctBoolean());

        return loadQuizResponse(quiz);
    }

    public QuizResponse deleteQuestion(UUID quizId, UUID questionId, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);
        QuizQuestion question = requireOwnedQuestion(quiz, questionId);
        questionRepository.delete(question);
        return loadQuizResponse(quiz);
    }

    // ---- student ----

    @Transactional(readOnly = true)
    public List<QuizResponse> listQuizzesForEnrolledCourse(UUID courseId, String studentEmail) {
        Course course = requireEnrolledCourse(courseId, studentEmail);
        return quizRepository.findByCourseIdAndPublishedAtIsNotNull(course.getId()).stream()
                .map(this::loadStudentQuizResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizForStudent(UUID quizId, String studentEmail) {
        return loadStudentQuizResponse(requireEnrolledPublishedQuiz(quizId, studentEmail));
    }

    // ---- shared helpers ----

    private void applyOptions(
            QuizQuestion question,
            QuestionType type,
            List<CreateQuestionRequest.OptionInput> options,
            Boolean correctBoolean
    ) {
        switch (type) {
            case MULTIPLE_CHOICE -> {
                if (options == null || options.size() < 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Multiple choice questions need at least 2 options");
                }
                long correctCount = options.stream().filter(CreateQuestionRequest.OptionInput::correct).count();
                if (correctCount != 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Multiple choice questions need exactly one correct option");
                }
                int position = 0;
                for (CreateQuestionRequest.OptionInput input : options) {
                    optionRepository.save(QuizQuestionOption.builder()
                            .question(question)
                            .text(input.text())
                            .correct(input.correct())
                            .position(position++)
                            .build());
                }
            }
            case TRUE_FALSE -> {
                if (correctBoolean == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "True/false questions need correctBoolean set");
                }
                optionRepository.save(QuizQuestionOption.builder()
                        .question(question).text("True").correct(correctBoolean).position(0).build());
                optionRepository.save(QuizQuestionOption.builder()
                        .question(question).text("False").correct(!correctBoolean).position(1).build());
            }
            case SHORT_ANSWER, ESSAY -> {
                // No options - free-text answer, graded by a tutor (optionally Sage-assisted).
            }
        }
    }

    private Topic resolveTopic(UUID topicId, Course course) {
        if (topicId == null) {
            return null;
        }
        return topicRepository.findById(topicId)
                .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    private GradeCategory resolveGradeCategory(UUID gradeCategoryId, Course course) {
        if (gradeCategoryId == null) {
            return null;
        }
        return gradeCategoryRepository.findById(gradeCategoryId)
                .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found"));
    }

    private QuizResponse loadQuizResponse(Quiz quiz) {
        List<QuestionResponse> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId()).stream()
                .map(question -> QuestionResponse.from(question, loadOptions(question)))
                .toList();
        return QuizResponse.from(quiz, questions);
    }

    private QuizResponse loadStudentQuizResponse(Quiz quiz) {
        List<QuestionResponse> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId()).stream()
                .map(question -> QuestionResponse.forStudent(question, loadStudentOptions(question)))
                .toList();
        return QuizResponse.from(quiz, questions);
    }

    private List<OptionResponse> loadOptions(QuizQuestion question) {
        return optionRepository.findByQuestionIdOrderByPositionAsc(question.getId()).stream()
                .map(OptionResponse::from)
                .toList();
    }

    private List<OptionResponse> loadStudentOptions(QuizQuestion question) {
        return optionRepository.findByQuestionIdOrderByPositionAsc(question.getId()).stream()
                .map(OptionResponse::forStudent)
                .toList();
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireStudentUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Course requireOwnedCourse(UUID courseId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's course exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Course requireEnrolledCourse(UUID courseId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Quiz requireOwnedQuiz(UUID quizId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        if (!quiz.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's quiz exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        return quiz;
    }

    private QuizQuestion requireOwnedQuestion(Quiz quiz, UUID questionId) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (!question.getQuiz().getId().equals(quiz.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }

        return question;
    }

    /**
     * Student access to a quiz requires both enrollment AND that it's published - either failure
     * masks as the same 404 as the other ownership checks in this codebase, so a student can't
     * distinguish "doesn't exist," "not enrolled," and "still a draft" from the response.
     */
    private Quiz requireEnrolledPublishedQuiz(UUID quizId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), quiz.getCourse().getId());
        if (!enrolled || quiz.getPublishedAt() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        return quiz;
    }
}
