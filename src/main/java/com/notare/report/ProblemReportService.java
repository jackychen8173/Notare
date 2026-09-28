package com.notare.report;

import com.notare.report.dto.CreateProblemReportRequest;
import com.notare.report.dto.ProblemReportResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProblemReportService {

    // Enough for any real reporter; stops a script from flooding the admin inbox.
    static final int MAX_REPORTS_PER_HOUR = 20;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final ProblemReportRepository problemReportRepository;
    private final UserRepository userRepository;

    public ProblemReportService(ProblemReportRepository problemReportRepository, UserRepository userRepository) {
        this.problemReportRepository = problemReportRepository;
        this.userRepository = userRepository;
    }

    public ProblemReportResponse createReport(CreateProblemReportRequest request, String userAgent, String reporterEmail) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        long recent = problemReportRepository.countByReporterIdAndCreatedAtAfter(
                reporter.getId(), LocalDateTime.now().minusHours(1));
        if (recent >= MAX_REPORTS_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You've sent a lot of reports recently. Please try again later.");
        }

        ProblemReport report = problemReportRepository.save(ProblemReport.builder()
                .reporter(reporter)
                .category(request.category())
                .message(request.message().trim())
                .pageUrl(request.pageUrl())
                .userAgent(userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH
                        ? userAgent.substring(0, MAX_USER_AGENT_LENGTH)
                        : userAgent)
                .build());
        return ProblemReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ProblemReportResponse> listReports(ProblemReportStatus status) {
        List<ProblemReport> reports = status == null
                ? problemReportRepository.findAllByOrderByCreatedAtDesc()
                : problemReportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(ProblemReportResponse::from).toList();
    }

    public ProblemReportResponse resolve(UUID id) {
        ProblemReport report = requireReport(id);
        if (report.getStatus() != ProblemReportStatus.RESOLVED) {
            report.setStatus(ProblemReportStatus.RESOLVED);
            report.setResolvedAt(LocalDateTime.now());
        }
        return ProblemReportResponse.from(report);
    }

    public ProblemReportResponse reopen(UUID id) {
        ProblemReport report = requireReport(id);
        report.setStatus(ProblemReportStatus.OPEN);
        report.setResolvedAt(null);
        return ProblemReportResponse.from(report);
    }

    private ProblemReport requireReport(UUID id) {
        return problemReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }
}
