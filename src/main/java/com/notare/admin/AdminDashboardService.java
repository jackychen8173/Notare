package com.notare.admin;

import com.notare.admin.dto.AdminDashboardResponse;
import com.notare.course.CourseRepository;
import com.notare.submission.SubmissionRepository;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            SubmissionRepository submissionRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                userRepository.countByRole(UserRole.TUTOR),
                userRepository.countByRole(UserRole.STUDENT),
                courseRepository.count(),
                submissionRepository.countByReleasedAtIsNull(),
                submissionRepository.countByReleasedAtIsNotNull()
        );
    }
}
