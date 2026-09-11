package com.notare.announcement;

import com.notare.announcement.dto.AnnouncementResponse;
import com.notare.announcement.dto.CreateAnnouncementRequest;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
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
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public AnnouncementResponse createAnnouncement(UUID courseId, CreateAnnouncementRequest request, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Course course = requireOwnedCourse(courseId, tutor);

        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot post announcements to an archived course");
        }

        Announcement announcement = Announcement.builder()
                .course(course)
                .tutor(tutor)
                .content(request.content())
                .build();

        announcementRepository.save(announcement);

        return AnnouncementResponse.from(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncements(UUID courseId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Course course = requireOwnedCourse(courseId, tutor);
        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(course.getId()).stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncementsForEnrolledCourse(UUID courseId, String studentEmail) {
        Course course = requireEnrolledCourse(courseId, studentEmail);
        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(course.getId()).stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    public void deleteAnnouncement(UUID announcementId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        if (!announcement.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's announcement exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        }

        announcementRepository.delete(announcement);
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

    private Course requireOwnedCourse(UUID courseId, User tutor) {
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
}
