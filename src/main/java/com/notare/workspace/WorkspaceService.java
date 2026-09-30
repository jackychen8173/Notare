package com.notare.workspace;

import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import com.notare.workspace.dto.PracticeFileResponse;
import com.notare.workspace.dto.SavePracticeFileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** A student's private practice files. Only ever visible to the student who owns them. */
@Service
@Transactional
public class WorkspaceService {

    static final int MAX_FILES = 50;

    private final PracticeFileRepository practiceFileRepository;
    private final UserRepository userRepository;

    public WorkspaceService(PracticeFileRepository practiceFileRepository, UserRepository userRepository) {
        this.practiceFileRepository = practiceFileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PracticeFileResponse> listFiles(String studentEmail) {
        User student = requireStudent(studentEmail);
        return practiceFileRepository.findByStudentIdOrderByUpdatedAtDesc(student.getId()).stream()
                .map(PracticeFileResponse::from)
                .toList();
    }

    public PracticeFileResponse createFile(SavePracticeFileRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        if (practiceFileRepository.countByStudentId(student.getId()) >= MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You can keep up to " + MAX_FILES + " practice files. Delete one to make room");
        }

        PracticeFile file = PracticeFile.builder()
                .student(student)
                .name(request.name().trim())
                .content(request.content())
                .build();
        practiceFileRepository.save(file);
        return PracticeFileResponse.from(file);
    }

    public PracticeFileResponse updateFile(UUID fileId, SavePracticeFileRequest request, String studentEmail) {
        PracticeFile file = requireOwnedFile(fileId, studentEmail);
        file.setName(request.name().trim());
        file.setContent(request.content());
        practiceFileRepository.saveAndFlush(file);
        return PracticeFileResponse.from(file);
    }

    public void deleteFile(UUID fileId, String studentEmail) {
        practiceFileRepository.delete(requireOwnedFile(fileId, studentEmail));
    }

    private PracticeFile requireOwnedFile(UUID fileId, String studentEmail) {
        User student = requireStudent(studentEmail);
        // 404 either way - never confirm another student's file exists
        return practiceFileRepository.findById(fileId)
                .filter(file -> file.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
