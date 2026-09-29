package com.notare.course;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.gradecategory.GradeCategory;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.material.Material;
import com.notare.material.MaterialRepository;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionOption;
import com.notare.quiz.QuizQuestionOptionRepository;
import com.notare.quiz.QuizQuestionRepository;
import com.notare.quiz.QuizRepository;
import com.notare.rubric.Rubric;
import com.notare.rubric.RubricCriterion;
import com.notare.rubric.RubricCriterionRepository;
import com.notare.rubric.RubricRepository;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Copies a course's authored content (topics, grade categories, materials, assignments + rubrics,
 * quizzes + questions + options) into another course. Copies are fully independent - nothing links
 * a copy back to its source. Student data (enrollments, submissions, quiz attempts) and course
 * activity (announcements, discussions, sessions) are deliberately not copied.
 */
@Component
@Transactional
public class CourseContentCopier {

    private final TopicRepository topicRepository;
    private final GradeCategoryRepository gradeCategoryRepository;
    private final MaterialRepository materialRepository;
    private final AssignmentRepository assignmentRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;

    public CourseContentCopier(
            TopicRepository topicRepository,
            GradeCategoryRepository gradeCategoryRepository,
            MaterialRepository materialRepository,
            AssignmentRepository assignmentRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizQuestionOptionRepository quizQuestionOptionRepository
    ) {
        this.topicRepository = topicRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
        this.materialRepository = materialRepository;
        this.assignmentRepository = assignmentRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizQuestionOptionRepository = quizQuestionOptionRepository;
    }

    public void copy(Course source, Course target) {
        // createdAt is carried over from the source rows (not reset to now) because topics, grade
        // categories and materials are listed in createdAt order - fresh timestamps written in one
        // tight loop could tie and scramble the tutor's ordering.
        Map<UUID, Topic> topics = new HashMap<>();
        for (Topic topic : topicRepository.findByCourseIdOrderByCreatedAtAsc(source.getId())) {
            topics.put(topic.getId(), topicRepository.save(Topic.builder()
                    .course(target)
                    .name(topic.getName())
                    .createdAt(topic.getCreatedAt())
                    .build()));
        }

        Map<UUID, GradeCategory> gradeCategories = new HashMap<>();
        for (GradeCategory category : gradeCategoryRepository.findByCourseIdOrderByCreatedAtAsc(source.getId())) {
            gradeCategories.put(category.getId(), gradeCategoryRepository.save(GradeCategory.builder()
                    .course(target)
                    .name(category.getName())
                    .weightPercent(category.getWeightPercent())
                    .createdAt(category.getCreatedAt())
                    .build()));
        }

        for (Material material : materialRepository.findByCourseIdOrderByCreatedAtDesc(source.getId())) {
            materialRepository.save(Material.builder()
                    .course(target)
                    .topic(mapTopic(topics, material.getTopic()))
                    .title(material.getTitle())
                    .description(material.getDescription())
                    .url(material.getUrl())
                    .type(material.getType())
                    .createdAt(material.getCreatedAt())
                    .build());
        }

        for (Assignment assignment : assignmentRepository.findByCourseId(source.getId())) {
            Assignment copy = assignmentRepository.save(Assignment.builder()
                    .course(target)
                    .topic(mapTopic(topics, assignment.getTopic()))
                    .gradeCategory(mapCategory(gradeCategories, assignment.getGradeCategory()))
                    .title(assignment.getTitle())
                    .description(assignment.getDescription())
                    .dueDate(assignment.getDueDate())
                    .allowResubmission(assignment.isAllowResubmission())
                    .build());
            rubricRepository.findByAssignmentId(assignment.getId())
                    .ifPresent(rubric -> copyRubric(rubric, copy));
        }

        for (Quiz quiz : quizRepository.findByCourseId(source.getId())) {
            copyQuiz(quiz, target, topics, gradeCategories);
        }
    }

    private void copyRubric(Rubric rubric, Assignment targetAssignment) {
        Rubric copy = rubricRepository.save(Rubric.builder()
                .assignment(targetAssignment)
                .title(rubric.getTitle())
                .build());
        for (RubricCriterion criterion : rubricCriterionRepository.findByRubricIdOrderByPositionAsc(rubric.getId())) {
            rubricCriterionRepository.save(RubricCriterion.builder()
                    .rubric(copy)
                    .name(criterion.getName())
                    .description(criterion.getDescription())
                    .pointsPossible(criterion.getPointsPossible())
                    .position(criterion.getPosition())
                    .build());
        }
    }

    private void copyQuiz(
            Quiz quiz,
            Course target,
            Map<UUID, Topic> topics,
            Map<UUID, GradeCategory> gradeCategories
    ) {
        // A published source quiz stays published in the copy: the main use is parallel sections of
        // the same class, where the tutor wants the same quizzes live in every section.
        Quiz copy = quizRepository.save(Quiz.builder()
                .course(target)
                .topic(mapTopic(topics, quiz.getTopic()))
                .gradeCategory(mapCategory(gradeCategories, quiz.getGradeCategory()))
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .publishedAt(quiz.getPublishedAt() != null ? LocalDateTime.now() : null)
                .createdAt(quiz.getCreatedAt())
                .build());

        for (QuizQuestion question : quizQuestionRepository.findByQuizIdOrderByPositionAsc(quiz.getId())) {
            QuizQuestion questionCopy = quizQuestionRepository.save(QuizQuestion.builder()
                    .quiz(copy)
                    .type(question.getType())
                    .prompt(question.getPrompt())
                    .pointsPossible(question.getPointsPossible())
                    .referenceAnswer(question.getReferenceAnswer())
                    .position(question.getPosition())
                    .build());
            for (QuizQuestionOption option : quizQuestionOptionRepository.findByQuestionIdOrderByPositionAsc(question.getId())) {
                quizQuestionOptionRepository.save(QuizQuestionOption.builder()
                        .question(questionCopy)
                        .text(option.getText())
                        .correct(option.isCorrect())
                        .position(option.getPosition())
                        .build());
            }
        }
    }

    private static Topic mapTopic(Map<UUID, Topic> copies, Topic source) {
        return source == null ? null : copies.get(source.getId());
    }

    private static GradeCategory mapCategory(Map<UUID, GradeCategory> copies, GradeCategory source) {
        return source == null ? null : copies.get(source.getId());
    }
}
