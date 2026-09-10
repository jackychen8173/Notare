package com.notare.topic;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.topic.dto.CreateTopicRequest;
import com.notare.topic.dto.RenameTopicRequest;
import com.notare.topic.dto.TopicResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public TopicService(TopicRepository topicRepository, CourseRepository courseRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public TopicResponse createTopic(UUID courseId, CreateTopicRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        requireNotArchived(course);

        Topic topic = Topic.builder()
                .course(course)
                .name(request.name())
                .build();

        topicRepository.save(topic);

        return TopicResponse.from(topic);
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> listTopics(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return topicRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                .map(TopicResponse::from)
                .toList();
    }

    public TopicResponse renameTopic(UUID topicId, RenameTopicRequest request, String tutorEmail) {
        Topic topic = requireOwnedTopic(topicId, tutorEmail);
        requireNotArchived(topic.getCourse());

        topic.setName(request.name());
        topicRepository.save(topic);

        return TopicResponse.from(topic);
    }

    public void deleteTopic(UUID topicId, String tutorEmail) {
        Topic topic = requireOwnedTopic(topicId, tutorEmail);
        requireNotArchived(topic.getCourse());

        topicRepository.delete(topic);
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
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

    private Topic requireOwnedTopic(UUID topicId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        if (!topic.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's topic exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
        }

        return topic;
    }

    private void requireNotArchived(Course course) {
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify topics in an archived course");
        }
    }
}
