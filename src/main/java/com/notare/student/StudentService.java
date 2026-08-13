package com.notare.student;

import com.notare.student.dto.CreateStudentRequest;
import com.notare.student.dto.StudentResponse;
import com.notare.student.dto.UpdateStudentRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StudentService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public StudentResponse createStudent(CreateStudentRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User student = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.STUDENT)
                .build();

        userRepository.save(student);

        return StudentResponse.from(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents() {
        return userRepository.findByRole(UserRole.STUDENT).stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID id) {
        return StudentResponse.from(requireStudent(id));
    }

    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
        User student = requireStudent(id);

        if (!student.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        student.setName(request.name());
        student.setEmail(request.email());
        userRepository.save(student);

        return StudentResponse.from(student);
    }

    private User requireStudent(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }
}
