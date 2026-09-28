package com.notare.admin;

import com.notare.admin.dto.AdminDashboardResponse;
import com.notare.course.CourseRepository;
import com.notare.report.ProblemReportRepository;
import com.notare.report.ProblemReportStatus;
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
    private final ProblemReportRepository problemReportRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            SubmissionRepository submissionRepository,
            ProblemReportRepository problemReportRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.problemReportRepository = problemReportRepository;
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                // Demo accounts (and their seeded content) are left out of every admin count.
                userRepository.countByRoleAndDemoFalse(UserRole.TUTOR),
                userRepository.countByRoleAndDemoFalse(UserRole.STUDENT),
                courseRepository.countByTutor_DemoFalse(),
                submissionRepository.countByReleasedAtIsNullAndStudent_DemoFalse(),
                submissionRepository.countByReleasedAtIsNotNullAndStudent_DemoFalse(),
                problemReportRepository.countByStatus(ProblemReportStatus.OPEN)
        );
    }
}
