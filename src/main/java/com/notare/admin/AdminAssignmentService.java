package com.notare.admin;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.dto.AssignmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminAssignmentService {

    private final AssignmentRepository assignmentRepository;

    public AdminAssignmentService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public AssignmentResponse getAssignment(UUID id) {
        return AssignmentResponse.from(requireAssignment(id));
    }

    private Assignment requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }
}
