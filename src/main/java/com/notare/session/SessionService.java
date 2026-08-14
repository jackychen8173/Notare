package com.notare.session;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.session.dto.CreateSessionRequest;
import com.notare.session.dto.SaveSessionNotesRequest;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.session.dto.SessionResponse;
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
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public SessionService(
            SessionRepository sessionRepository,
            SessionNoteRepository sessionNoteRepository,
            UserRepository userRepository,
            CourseRepository courseRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionNoteRepository = sessionNoteRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public SessionResponse createSession(CreateSessionRequest request, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);

        User student = userRepository.findById(request.studentId())
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            if (!course.getTutor().getId().equals(tutor.getId())) {
                // 404, not 403 - avoid confirming another tutor's course exists
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
            }
        }

        Session session = Session.builder()
                .tutor(tutor)
                .student(student)
                .course(course)
                .date(request.date())
                .subject(request.subject())
                .duration(request.duration())
                .status(SessionStatus.SCHEDULED)
                .build();

        sessionRepository.save(session);

        return SessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        return sessionRepository.findByTutorId(tutor.getId()).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessionsForStudent(String studentEmail) {
        User student = requireStudentUser(studentEmail);
        return sessionRepository.findByStudentId(student.getId()).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, String tutorEmail) {
        return SessionResponse.from(requireOwnedSession(sessionId, tutorEmail));
    }

    public SessionResponse completeSession(UUID sessionId, String tutorEmail) {
        Session session = requireOwnedSession(sessionId, tutorEmail);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot complete a cancelled session");
        }

        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        return SessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public SessionNoteResponse getSessionNotes(UUID sessionId, String tutorEmail) {
        Session session = requireOwnedSession(sessionId, tutorEmail);
        return sessionNoteRepository.findBySessionId(session.getId())
                .map(SessionNoteResponse::from)
                .orElse(null);
    }

    public SessionNoteResponse saveSessionNotes(UUID sessionId, SaveSessionNotesRequest request, String tutorEmail) {
        Session session = requireOwnedSession(sessionId, tutorEmail);

        SessionNote note = sessionNoteRepository.findBySessionId(session.getId())
                .orElseGet(() -> SessionNote.builder().session(session).build());

        note.setRawNotes(request.rawNotes());
        sessionNoteRepository.save(note);

        return SessionNoteResponse.from(note);
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

    private Session requireOwnedSession(UUID sessionId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's session exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        return session;
    }
}
