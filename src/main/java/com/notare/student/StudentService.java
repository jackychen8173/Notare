package com.notare.student;

import com.notare.course.EnrollmentRepository;
import com.notare.student.dto.StudentResponse;
import com.notare.student.dto.UpdateStudentRequest;
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
public class StudentService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentService(UserRepository userRepository, EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        return enrollmentRepository.findDistinctStudentsByCourseTutorId(tutor.getId()).stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID id, String tutorEmail) {
        return StudentResponse.from(requireVisibleStudent(id, tutorEmail));
    }

    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request, String tutorEmail) {
        User student = requireVisibleStudent(id, tutorEmail);

        if (!student.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        student.setName(request.name());
        student.setEmail(request.email());
        userRepository.save(student);

        return StudentResponse.from(student);
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireVisibleStudent(UUID id, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        User student = userRepository.findById(id)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // 404, not 403 - avoid confirming a student exists who isn't in one of this tutor's courses
        if (!enrollmentRepository.existsByStudentIdAndCourse_Tutor_Id(student.getId(), tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        return student;
    }
}
