package com.notare.file;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.file.dto.FileResponse;
import com.notare.material.Material;
import com.notare.material.MaterialRepository;
import com.notare.material.MaterialType;
import com.notare.material.dto.MaterialResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uploads and who may download them. Course materials: the tutor uploads, the tutor and enrolled
 * students download. A student's assignment files: drafts only the student sees, which become part
 * of their submission when they submit (SubmissionService), after which the course's tutor can
 * download them too. Downloads are short-lived links, so a link someone copies stops working.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    static final Duration LINK_LIFETIME = Duration.ofMinutes(10);
    static final int MAX_DRAFTS_PER_ASSIGNMENT = 10;

    private final FileStorage storage;
    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MaterialRepository materialRepository;
    private final TopicRepository topicRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final long maxBytes;
    private final long quotaBytes;

    public FileService(
            FileStorage storage,
            StoredFileRepository fileRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            MaterialRepository materialRepository,
            TopicRepository topicRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            @Value("${files.max-bytes}") long maxBytes,
            @Value("${files.quota-bytes}") long quotaBytes
    ) {
        this.storage = storage;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.materialRepository = materialRepository;
        this.topicRepository = topicRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.maxBytes = maxBytes;
        this.quotaBytes = quotaBytes;
    }

    // ---- tutors: materials

    @Transactional
    public MaterialResponse uploadMaterial(UUID courseId, MultipartFile upload, String title, String description,
                                           UUID topicId, String tutorEmail) {
        User tutor = requireUser(tutorEmail, UserRole.TUTOR);
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> candidate.getTutor().getId().equals(tutor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add materials to an archived course");
        }
        Topic topic = topicId == null ? null : topicRepository.findById(topicId)
                .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        String name = FileNames.clean(upload.getOriginalFilename());
        Material material = materialRepository.save(Material.builder()
                .course(course)
                .topic(topic)
                .title(title == null || title.isBlank() ? name : title.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .type(MaterialType.FILE)
                .build());
        StoredFile file = store(upload, tutor, course, builder -> builder.material(material));
        return MaterialResponse.from(material, FileResponse.from(file));
    }

    /** Called when a material is deleted: removes its file from storage too. */
    @Transactional
    public void deleteForMaterial(Material material) {
        for (StoredFile file : fileRepository.findByMaterialId(material.getId())) {
            deleteObject(file);
            fileRepository.delete(file);
        }
    }

    // ---- students: files for their next submission

    @Transactional
    public FileResponse uploadDraft(UUID assignmentId, MultipartFile upload, String studentEmail) {
        User student = requireUser(studentEmail, UserRole.STUDENT);
        Assignment assignment = requireOpenAssignment(assignmentId, student);
        if (!assignment.isAcceptingSubmissions(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This assignment is closed and no longer accepts submissions");
        }
        if (fileRepository.findByAssignmentIdAndUploaderIdAndSubmissionIsNullOrderByCreatedAtAsc(assignmentId, student.getId()).size()
                >= MAX_DRAFTS_PER_ASSIGNMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can attach up to " + MAX_DRAFTS_PER_ASSIGNMENT + " files");
        }
        StoredFile file = store(upload, student, assignment.getCourse(), builder -> builder.assignment(assignment));
        return FileResponse.from(file);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> drafts(UUID assignmentId, String studentEmail) {
        User student = requireUser(studentEmail, UserRole.STUDENT);
        requireOpenAssignment(assignmentId, student);
        return fileRepository.findByAssignmentIdAndUploaderIdAndSubmissionIsNullOrderByCreatedAtAsc(assignmentId, student.getId())
                .stream().map(FileResponse::from).toList();
    }

    /** Files of one submission: for its student, and for the course's tutor. */
    @Transactional(readOnly = true)
    public List<FileResponse> submissionFiles(UUID submissionId, String email) {
        User user = requireAnyUser(email);
        Submission submission = submissionRepository.findById(submissionId)
                .filter(candidate -> candidate.getStudent().getId().equals(user.getId())
                        || candidate.getAssignment().getCourse().getTutor().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        return fileRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId()).stream().map(FileResponse::from).toList();
    }

    // ---- everyone

    @Transactional(readOnly = true)
    public String link(UUID fileId, String email) {
        User user = requireAnyUser(email);
        StoredFile file = fileRepository.findById(fileId)
                .filter(candidate -> canDownload(candidate, user))
                // 404, not 403 - don't confirm someone else's file exists
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        return storage.downloadUrl(file.getObjectKey(), file.getFileName(), file.getContentType(),
                FileNames.inline(file.getContentType()), LINK_LIFETIME);
    }

    /** Only a student's own draft can be removed; materials go with their material, submitted files stay. */
    @Transactional
    public void delete(UUID fileId, String email) {
        User user = requireAnyUser(email);
        StoredFile file = fileRepository.findById(fileId)
                .filter(candidate -> canDownload(candidate, user))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getMaterial() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete the material instead");
        }
        if (file.getSubmission() != null || !file.getUploader().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Files in a submission can't be removed");
        }
        deleteObject(file);
        fileRepository.delete(file);
    }

    private boolean canDownload(StoredFile file, User user) {
        Course course = file.getCourse();
        boolean tutor = course.getTutor().getId().equals(user.getId());
        if (file.getMaterial() != null) {
            return tutor || (user.getRole() == UserRole.STUDENT
                    && enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), course.getId()));
        }
        // An assignment file: its student always; the tutor once it's part of a submission.
        return file.getUploader().getId().equals(user.getId()) || (tutor && file.getSubmission() != null);
    }

    // ---- storing

    private StoredFile store(MultipartFile upload, User uploader, Course course,
                             java.util.function.UnaryOperator<StoredFile.StoredFileBuilder> owner) {
        if (!storage.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File uploads aren't set up yet");
        }
        if (upload == null || upload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a file to upload");
        }
        if (upload.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Files can be up to 10 MB");
        }
        if (fileRepository.totalBytesUploadedBy(uploader.getId()) + upload.getSize() > quotaBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "You've used your " + (quotaBytes / (1024 * 1024)) + " MB of uploads. Delete some files first");
        }

        String key = "files/" + UUID.randomUUID();
        String contentType = FileNames.contentType(upload.getContentType());
        try (InputStream content = upload.getInputStream()) {
            storage.put(key, contentType, upload.getSize(), content);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read the upload");
        }
        try {
            return fileRepository.save(owner.apply(StoredFile.builder()
                            .uploader(uploader)
                            .course(course)
                            .objectKey(key)
                            .fileName(FileNames.clean(upload.getOriginalFilename()))
                            .contentType(contentType)
                            .sizeBytes(upload.getSize()))
                    .build());
        } catch (RuntimeException e) {
            storage.delete(key);
            throw e;
        }
    }

    private void deleteObject(StoredFile file) {
        try {
            storage.delete(file.getObjectKey());
        } catch (RuntimeException e) {
            // The row goes either way; an orphaned object only costs storage. No file names in the log.
            log.warn("Couldn't delete stored object for file {}", file.getId(), e);
        }
    }

    private Assignment requireOpenAssignment(UUID assignmentId, User student) {
        return assignmentRepository.findById(assignmentId)
                .filter(candidate -> enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), candidate.getCourse().getId()))
                .filter(candidate -> candidate.isVisibleToStudents(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private User requireUser(String email, UserRole role) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireAnyUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
