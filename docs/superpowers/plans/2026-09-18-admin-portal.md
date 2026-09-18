# Admin Portal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a third `ADMIN` role and an admin-only portal (backend `/api/admin/*` + frontend `/admin/*`) for read oversight of every tutor's courses/assignments/submissions and soft-deactivate/reactivate account management over tutors and students.

**Architecture:** New parallel `com.notare.admin` backend package (controllers + thin services with no ownership filtering, reusing existing DTOs' `from()` factories) sitting alongside the existing tutor-scoped code untouched. New parallel `app/admin/` frontend segment following the exact `(tutor)`/`student` layout pattern, reusing the already-generalized `Sidebar` and existing display components wherever DTO shapes already match.

**Tech Stack:** Spring Boot 4.1 / Java 21 / Spring Security 7 (`@PreAuthorize`) / Flyway / Postgres — Next.js 16 / React 19 / TanStack Query v5 / React Hook Form + Zod / shadcn (Base UI) / Tailwind v4.

**Spec:** `docs/superpowers/specs/2026-09-18-admin-portal-design.md`

## Global Constraints

- No `ON DELETE CASCADE` anywhere, no hard delete anywhere — soft `active` flag only (dropped from spec mid-brainstorm).
- No API path may ever create an `ADMIN` user — `AuthService.register()` must reject `role == ADMIN` outright. The only way an admin row exists is a human-run `INSERT` (example provided, never executed by the agent).
- Admin's cross-tutor reads are **parallel** controllers/services with zero ownership filtering — never add `ADMIN` to an existing `@PreAuthorize` or thread a bypass through an existing `requireOwnedX`/`requireVisibleX` method.
- Admin user listing/detail excludes `ADMIN`-role accounts entirely — this portal is for overseeing "teachers and students," not other admins.
- No DB CLI commands (`psql` etc.) and no Flyway `migrate`/`clean`/etc. may be run by the agent — migrations are authored as files only, applied by the user.
- Backend cannot be run in this environment (no Maven/JDK) — verification is `./mvnw compile` plus reading new code against the actual DTOs/repository methods it calls, per every prior backend-touching phase's standing note.
- Frontend verification is `npm run build` + `npm run lint` + a live dev-server smoke test of every new route, matching every prior frontend phase.

---

## Task 1: Data model — ADMIN role + active flag

**Files:**
- Create: `src/main/resources/db/migration/V14__add_admin_role.sql`
- Create: `src/main/resources/db/migration/V15__add_user_active_flag.sql`
- Modify: `src/main/java/com/notare/user/UserRole.java`
- Modify: `src/main/java/com/notare/user/User.java`

**Interfaces:**
- Produces: `UserRole.ADMIN` enum constant; `User.isActive()` / `User.setActive(boolean)` (Lombok `@Data`), defaulting to `true` via `@Builder.Default` so every existing `User.builder()...build()` call site (notably `AuthService.register()`) keeps working unchanged.

- [ ] **Step 1: Write the role-constraint migration**

```sql
-- V14__add_admin_role.sql
-- Finds the existing CHECK constraint on users.role (auto-named by Postgres,
-- e.g. users_role_check) dynamically rather than assuming its exact name,
-- since it was created via inline column-level CHECK syntax in V1.
DO $$
DECLARE
    existing_constraint text;
BEGIN
    SELECT con.conname INTO existing_constraint
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'users'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%role%';

    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', existing_constraint);
    END IF;
END $$;

ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('TUTOR', 'STUDENT', 'ADMIN'));
```

- [ ] **Step 2: Write the active-flag migration**

```sql
-- V15__add_user_active_flag.sql
ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
```

- [ ] **Step 3: Add `ADMIN` to the `UserRole` enum**

```java
package com.notare.user;

public enum UserRole {
    TUTOR,
    STUDENT,
    ADMIN
}
```

- [ ] **Step 4: Add the `active` field to `User`**

Add these imports to `src/main/java/com/notare/user/User.java`:

```java
import lombok.Builder;
```

(already imported — no change needed there). Add the field just below `role`:

```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
```

- [ ] **Step 5: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`, no changes to any other class needed yet (nothing references `active` or `ADMIN` outside this file until later tasks).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V14__add_admin_role.sql src/main/resources/db/migration/V15__add_user_active_flag.sql src/main/java/com/notare/user/UserRole.java src/main/java/com/notare/user/User.java
git commit -m "Add ADMIN role and users.active flag (migrations only, not yet applied)"
```

---

## Task 2: Close the self-registration hole + admin seed example

**Files:**
- Modify: `src/main/java/com/notare/auth/AuthService.java`
- Create: `docs/admin-seed-example.sql`

**Interfaces:**
- Consumes: `UserRole.ADMIN` (Task 1).
- Produces: `AuthService.register()` now throws `403 FORBIDDEN` for `role == ADMIN`, guaranteeing no API path can ever create one.

- [ ] **Step 1: Reject `ADMIN` at registration**

In `src/main/java/com/notare/auth/AuthService.java`, add the check as the first line of `register()`:

```java
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot self-register");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        ...
```

Add the import:

```java
import com.notare.user.UserRole;
```

- [ ] **Step 2: Write the manual admin-seed example**

This is not a Flyway migration — it's a one-off example the user runs by hand against their own database to create the first admin. Never executed by the agent (DB-mutation harness rule).

```sql
-- docs/admin-seed-example.sql
--
-- Manual, one-time example for creating the first ADMIN account. Run this
-- yourself against the target database (local or production) — it is never
-- executed by Claude, per the "no direct DB mutations" project rule.
--
-- 1. Generate a bcrypt hash for the admin's password, e.g. in a local Java/Node
--    REPL: new BCryptPasswordEncoder().encode("your-password-here")
-- 2. Replace the placeholders below and run the INSERT.

INSERT INTO users (id, name, email, password, role, active, created_at)
VALUES (
    gen_random_uuid(),
    'Admin Name',
    'admin@example.com',
    '$2a$10$REPLACE_WITH_A_REAL_BCRYPT_HASH',
    'ADMIN',
    true,
    now()
);
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Verify by reading**

Confirm `RegisterRequest.role()` is `@NotNull UserRole` (so `ADMIN` deserializes successfully before the new check runs, rather than failing validation first) — read `src/main/java/com/notare/auth/dto/RegisterRequest.java` and confirm the check in Step 1 sits before the `existsByEmail` check so the 403 always fires first regardless of email state.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/notare/auth/AuthService.java docs/admin-seed-example.sql
git commit -m "Reject ADMIN role at self-registration; document manual admin seeding"
```

---

## Task 3: Kill live sessions on deactivation

**Files:**
- Modify: `src/main/java/com/notare/auth/JwtAuthenticationFilter.java`

**Interfaces:**
- Consumes: `User.isActive()` (Task 1), `UserRepository.findByEmail(String)` (existing).
- Produces: requests from a deactivated user's still-valid JWT are now treated as unauthenticated (no `Authentication` set in the `SecurityContext`), which — combined with the existing `.anonymous(AbstractHttpConfigurer::disable)` + `.anyRequest().authenticated()` in `SecurityConfig` — falls through to the existing `401` entry point automatically. No new exception handling needed.

- [ ] **Step 1: Inject `UserRepository` and check `active` before setting authentication**

```java
package com.notare.auth;

import com.notare.user.User;
import com.notare.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                if (jwtUtil.isValid(token)) {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);

                    boolean active = userRepository.findByEmail(email)
                            .map(User::isActive)
                            .orElse(false);

                    if (active) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Verify by reading**

Confirm every currently-passing request path is unaffected for `active = true` users (the default for every existing row post-migration): the new `findByEmail` lookup only changes behavior when the user is missing or `active = false`, both previously-impossible states for any pre-existing account.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/notare/auth/JwtAuthenticationFilter.java
git commit -m "Reject requests from deactivated users at the JWT filter"
```

---

## Task 4: Admin user directory + account management

**Files:**
- Create: `src/main/java/com/notare/admin/dto/AdminUserResponse.java`
- Create: `src/main/java/com/notare/admin/AdminUserService.java`
- Create: `src/main/java/com/notare/admin/AdminUserController.java`
- Modify: `src/main/java/com/notare/user/UserRepository.java`

**Interfaces:**
- Consumes: `User`, `UserRole`, `UserRepository` (existing + Task 1's `active`).
- Produces: `AdminUserResponse.from(User)`; `AdminUserService.listUsers(UserRole role, Boolean active)`, `.getUser(UUID)`, `.deactivateUser(UUID)`, `.reactivateUser(UUID)` — all excluding `ADMIN`-role rows; `GET/PATCH /api/admin/users/**`.

- [ ] **Step 1: Add a `countByRole` repository method (used later by the dashboard, added here since it lives on the same file)**

```java
package com.notare.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    long countByRole(UserRole role);
}
```

- [ ] **Step 2: Write `AdminUserResponse`**

```java
package com.notare.admin.dto;

import com.notare.user.User;
import com.notare.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
```

- [ ] **Step 3: Write `AdminUserService`**

```java
package com.notare.admin;

import com.notare.admin.dto.AdminUserResponse;
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
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserRole role, Boolean active) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> active == null || user.isActive() == active)
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID id) {
        return AdminUserResponse.from(requireManageableUser(id));
    }

    public AdminUserResponse deactivateUser(UUID id) {
        User user = requireManageableUser(id);
        user.setActive(false);
        userRepository.save(user);
        return AdminUserResponse.from(user);
    }

    public AdminUserResponse reactivateUser(UUID id) {
        User user = requireManageableUser(id);
        user.setActive(true);
        userRepository.save(user);
        return AdminUserResponse.from(user);
    }

    private User requireManageableUser(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
```

- [ ] **Step 4: Write `AdminUserController`**

```java
package com.notare.admin;

import com.notare.admin.dto.AdminUserResponse;
import com.notare.common.ApiResponse;
import com.notare.user.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.listUsers(role, active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.deactivateUser(id)));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> reactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.reactivateUser(id)));
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Verify by reading**

Confirm `requireManageableUser` filters out `ADMIN` rows the same way for all four methods (get/deactivate/reactivate), so an admin can never fetch or mutate another admin account through this controller, matching the Global Constraints.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/notare/admin/dto/AdminUserResponse.java src/main/java/com/notare/admin/AdminUserService.java src/main/java/com/notare/admin/AdminUserController.java src/main/java/com/notare/user/UserRepository.java
git commit -m "Add admin user directory and deactivate/reactivate endpoints"
```

---

## Task 5: Admin course oversight

**Files:**
- Create: `src/main/java/com/notare/admin/AdminCourseService.java`
- Create: `src/main/java/com/notare/admin/AdminCourseController.java`

**Interfaces:**
- Consumes: `CourseRepository`, `EnrollmentRepository`, `AssignmentRepository` (all existing), `CourseResponse.from()`, `StudentResponse.from()`, `AssignmentResponse.from()` (all existing static factories, unchanged).
- Produces: `GET /api/admin/courses`, `GET /api/admin/courses/{id}`, `GET /api/admin/courses/{id}/students`, `GET /api/admin/courses/{id}/assignments`.

- [ ] **Step 1: Write `AdminCourseService`**

```java
package com.notare.admin;

import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.dto.AssignmentResponse;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.course.dto.CourseResponse;
import com.notare.student.dto.StudentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;

    public AdminCourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<CourseResponse> listCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    public CourseResponse getCourse(UUID id) {
        return CourseResponse.from(requireCourse(id));
    }

    public List<StudentResponse> listStudents(UUID id) {
        Course course = requireCourse(id);
        return enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(enrollment -> StudentResponse.from(enrollment.getStudent()))
                .toList();
    }

    public List<AssignmentResponse> listAssignments(UUID id) {
        Course course = requireCourse(id);
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    private Course requireCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }
}
```

- [ ] **Step 2: Write `AdminCourseController`**

```java
package com.notare.admin;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.common.ApiResponse;
import com.notare.course.dto.CourseResponse;
import com.notare.student.dto.StudentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses() {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listCourses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.getCourse(id)));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listStudents(id)));
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listAssignments(id)));
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Verify by reading**

Confirm none of `AdminCourseService`'s methods take a `tutorEmail`/`Authentication` parameter or call any `requireOwned*` method — this is the "parallel controller, zero ownership filtering" approach from the spec, distinct from `CourseService`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/notare/admin/AdminCourseService.java src/main/java/com/notare/admin/AdminCourseController.java
git commit -m "Add admin course oversight endpoints (list, detail, roster, assignments)"
```

---

## Task 6: Admin assignment detail

**Files:**
- Create: `src/main/java/com/notare/admin/AdminAssignmentService.java`
- Create: `src/main/java/com/notare/admin/AdminAssignmentController.java`

**Interfaces:**
- Consumes: `AssignmentRepository`, `AssignmentResponse.from()` (existing, unchanged).
- Produces: `GET /api/admin/assignments/{id}`.

- [ ] **Step 1: Write `AdminAssignmentService`**

```java
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
```

- [ ] **Step 2: Write `AdminAssignmentController`**

```java
package com.notare.admin;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

    private final AdminAssignmentService adminAssignmentService;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService) {
        this.adminAssignmentService = adminAssignmentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminAssignmentService.getAssignment(id)));
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/notare/admin/AdminAssignmentService.java src/main/java/com/notare/admin/AdminAssignmentController.java
git commit -m "Add admin assignment detail endpoint"
```

---

## Task 7: Admin submission oversight

**Files:**
- Create: `src/main/java/com/notare/admin/AdminSubmissionService.java`
- Create: `src/main/java/com/notare/admin/AdminSubmissionController.java`
- Modify: `src/main/java/com/notare/submission/SubmissionRepository.java`

**Interfaces:**
- Consumes: `SubmissionRepository`, `AssignmentRepository`, `SubmissionCriterionScoreRepository` (all existing), `SubmissionResponse.from(Submission, List<RubricScoreItem>)` — the tutor-facing factory, **not** `forStudent()`, per the spec's "admin sees everything" decision.
- Produces: `GET /api/admin/submissions/{id}`, `GET /api/admin/assignments/{id}/submissions`.

- [ ] **Step 1: Add count-by-released methods to `SubmissionRepository` (used by the dashboard in Task 8, added here since Task 8 depends on this file)**

```java
package com.notare.submission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByStudentIdAndAssignment_Course_Tutor_Id(UUID studentId, UUID tutorId);

    List<Submission> findByAssignmentId(UUID assignmentId);

    long countByFeedbackStatusAndAssignment_Course_Tutor_Id(FeedbackStatus feedbackStatus, UUID tutorId);

    Optional<Submission> findFirstByStudentIdAndAssignmentIdOrderBySubmittedAtDesc(
            UUID studentId, UUID assignmentId);

    List<Submission> findByAssignment_Course_Tutor_Id(UUID tutorId);

    long countByReleasedAtIsNull();

    long countByReleasedAtIsNotNull();
}
```

- [ ] **Step 2: Write `AdminSubmissionService`**

```java
package com.notare.admin;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionCriterionScoreRepository;
import com.notare.submission.SubmissionRepository;
import com.notare.submission.dto.RubricScoreItem;
import com.notare.submission.dto.SubmissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionCriterionScoreRepository criterionScoreRepository;

    public AdminSubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            SubmissionCriterionScoreRepository criterionScoreRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.criterionScoreRepository = criterionScoreRepository;
    }

    public SubmissionResponse getSubmission(UUID id) {
        Submission submission = requireSubmission(id);
        return SubmissionResponse.from(submission, loadRubricScores(submission.getId()));
    }

    public List<SubmissionResponse> listSubmissionsForAssignment(UUID assignmentId) {
        Assignment assignment = requireAssignment(assignmentId);
        return submissionRepository.findByAssignmentId(assignment.getId()).stream()
                .map(submission -> SubmissionResponse.from(submission, loadRubricScores(submission.getId())))
                .toList();
    }

    private List<RubricScoreItem> loadRubricScores(UUID submissionId) {
        return criterionScoreRepository.findBySubmissionId(submissionId).stream()
                .map(score -> new RubricScoreItem(
                        score.getCriterion().getId(),
                        score.getCriterion().getName(),
                        score.getPointsAwarded(),
                        score.getCriterion().getPointsPossible()
                ))
                .toList();
    }

    private Submission requireSubmission(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private Assignment requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }
}
```

- [ ] **Step 3: Write `AdminSubmissionController`**

```java
package com.notare.admin;

import com.notare.common.ApiResponse;
import com.notare.submission.dto.SubmissionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubmissionController {

    private final AdminSubmissionService adminSubmissionService;

    public AdminSubmissionController(AdminSubmissionService adminSubmissionService) {
        this.adminSubmissionService = adminSubmissionService;
    }

    @GetMapping("/api/admin/submissions/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminSubmissionService.getSubmission(id)));
    }

    @GetMapping("/api/admin/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> listSubmissionsForAssignment(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSubmissionService.listSubmissionsForAssignment(id)));
    }
}
```

(Mirrors the existing `SubmissionController`, which likewise owns an `/api/assignments/{id}/submissions` route despite the URL starting with "assignments.")

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Verify by reading**

Confirm `SubmissionResponse.from(...)` (not `.forStudent(...)`) is used in both methods — this is the deliberate "admin sees Sage feedback/grade/rubric scores unconditionally" behavior from the spec.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/notare/admin/AdminSubmissionService.java src/main/java/com/notare/admin/AdminSubmissionController.java src/main/java/com/notare/submission/SubmissionRepository.java
git commit -m "Add admin submission oversight endpoints"
```

---

## Task 8: Admin dashboard

**Files:**
- Create: `src/main/java/com/notare/admin/dto/AdminDashboardResponse.java`
- Create: `src/main/java/com/notare/admin/AdminDashboardService.java`
- Create: `src/main/java/com/notare/admin/AdminDashboardController.java`

**Interfaces:**
- Consumes: `UserRepository.countByRole()` (Task 4), `CourseRepository.count()` (inherited from `JpaRepository`), `SubmissionRepository.countByReleasedAtIsNull()/countByReleasedAtIsNotNull()` (Task 7).
- Produces: `GET /api/admin/dashboard`.

- [ ] **Step 1: Write `AdminDashboardResponse`**

```java
package com.notare.admin.dto;

public record AdminDashboardResponse(
        long tutorCount,
        long studentCount,
        long courseCount,
        long submissionsPending,
        long submissionsReleased
) {
}
```

- [ ] **Step 2: Write `AdminDashboardService`**

```java
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
```

- [ ] **Step 3: Write `AdminDashboardController`**

```java
package com.notare.admin;

import com.notare.admin.dto.AdminDashboardResponse;
import com.notare.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getDashboard()));
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS`. This completes the backend — all of `com.notare.admin` now compiles together.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/notare/admin/dto/AdminDashboardResponse.java src/main/java/com/notare/admin/AdminDashboardService.java src/main/java/com/notare/admin/AdminDashboardController.java
git commit -m "Add admin dashboard endpoint with platform-wide counts"
```

---

## Task 9: Frontend types, auth guard, and login redirect for ADMIN

**Files:**
- Modify: `frontend/types/user.ts`
- Create: `frontend/types/admin.ts`
- Modify: `frontend/hooks/useAuthGuard.ts`
- Modify: `frontend/app/(auth)/login/page.tsx`

**Interfaces:**
- Produces: `UserRole` now includes `"ADMIN"`; `AdminUser`, `AdminDashboardStats` types; `useAuthGuard` redirects correctly for all three roles.

- [ ] **Step 1: Add `ADMIN` to `UserRole`**

```typescript
// frontend/types/user.ts
export type UserRole = "TUTOR" | "STUDENT" | "ADMIN";

export interface AuthSession {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface Student {
  id: string;
  name: string;
  email: string;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  createdAt: string;
}
```

- [ ] **Step 2: Write `frontend/types/admin.ts`**

```typescript
export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: "TUTOR" | "STUDENT";
  active: boolean;
  createdAt: string;
}

export interface AdminDashboardStats {
  tutorCount: number;
  studentCount: number;
  courseCount: number;
  submissionsPending: number;
  submissionsReleased: number;
}
```

- [ ] **Step 3: Generalize `useAuthGuard`'s redirect for three roles**

```typescript
// frontend/hooks/useAuthGuard.ts
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getSession } from "@/lib/auth";
import type { UserRole } from "@/types/user";

const ROLE_HOME: Record<UserRole, string> = {
  TUTOR: "/dashboard",
  STUDENT: "/student/dashboard",
  ADMIN: "/admin/dashboard",
};

export function useAuthGuard(requiredRole: UserRole): boolean {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
    } else if (session.role !== requiredRole) {
      router.replace(ROLE_HOME[session.role]);
    } else {
      // getSession() reads localStorage, unavailable during SSR — this can only
      // be determined after mount, so it isn't derivable during render.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setAuthorized(true);
    }
  }, [router, requiredRole]);

  return authorized;
}
```

- [ ] **Step 4: Add the ADMIN branch to the login redirect**

In `frontend/app/(auth)/login/page.tsx`, replace:

```typescript
      router.push(session.role === "TUTOR" ? "/dashboard" : "/student/dashboard");
```

with:

```typescript
      const roleHome: Record<typeof session.role, string> = {
        TUTOR: "/dashboard",
        STUDENT: "/student/dashboard",
        ADMIN: "/admin/dashboard",
      };
      router.push(roleHome[session.role]);
```

- [ ] **Step 5: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; no other file references the old two-role redirect ternary (the register page's redirect is unaffected since `ADMIN` can never come back from `/api/auth/register`).

- [ ] **Step 6: Commit**

```bash
git add frontend/types/user.ts frontend/types/admin.ts frontend/hooks/useAuthGuard.ts "frontend/app/(auth)/login/page.tsx"
git commit -m "Add ADMIN role to frontend types, auth guard, and login redirect"
```

---

## Task 10: Small connective fixes — StudentRow href + ProfileView role label

**Files:**
- Modify: `frontend/components/student/StudentRow.tsx`
- Modify: `frontend/components/user/ProfileView.tsx`

**Interfaces:**
- Produces: `StudentRow` accepts an optional `href` prop (default preserves the existing `/students/${id}` tutor link) so the admin course-roster view (Task 16) can point it at `/admin/users/${id}` instead — the same fix pattern already applied to `SessionCard`/`AssignmentCard` in phase 12 for the identical reason. `ProfileView` shows a correct role label for all three roles instead of assuming binary TUTOR/STUDENT.

- [ ] **Step 1: Add an optional `href` prop to `StudentRow`**

```typescript
// frontend/components/student/StudentRow.tsx
import Link from "next/link";

import { StudentAvatar } from "@/components/student/StudentAvatar";
import { Button } from "@/components/ui/button";
import { TableCell, TableRow } from "@/components/ui/table";
import type { Student } from "@/types/user";

interface StudentRowProps {
  student: Student;
  href?: string;
  onRemove?: () => void;
  removePending?: boolean;
}

export function StudentRow({ student, href, onRemove, removePending }: StudentRowProps) {
  return (
    <TableRow>
      <TableCell>
        <Link href={href ?? `/students/${student.id}`} className="flex items-center gap-3">
          <StudentAvatar name={student.name} size="sm" />
          <span className="font-medium text-foreground">{student.name}</span>
        </Link>
      </TableCell>
      <TableCell className="text-muted-foreground">{student.email}</TableCell>
      <TableCell className="text-muted-foreground">
        {new Date(student.createdAt).toLocaleDateString()}
      </TableCell>
      {onRemove ? (
        <TableCell>
          <Button variant="ghost" size="sm" disabled={removePending} onClick={onRemove}>
            Remove
          </Button>
        </TableCell>
      ) : null}
    </TableRow>
  );
}
```

- [ ] **Step 2: Fix the role label in `ProfileView`**

In `frontend/components/user/ProfileView.tsx`, replace:

```typescript
          <Badge variant="outline">{profile.data.role === "TUTOR" ? "Tutor" : "Student"}</Badge>
```

with:

```typescript
          <Badge variant="outline">{roleLabel[profile.data.role]}</Badge>
```

and add, above the `ProfileView` function:

```typescript
const roleLabel: Record<"TUTOR" | "STUDENT" | "ADMIN", string> = {
  TUTOR: "Tutor",
  STUDENT: "Student",
  ADMIN: "Admin",
};
```

- [ ] **Step 3: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; existing call sites of `StudentRow` (students list, course roster) are unaffected since `href` is optional and defaults to the prior behavior.

- [ ] **Step 4: Commit**

```bash
git add frontend/components/student/StudentRow.tsx frontend/components/user/ProfileView.tsx
git commit -m "Generalize StudentRow's link target and fix ProfileView's role label for ADMIN"
```

---

## Task 11: Admin hooks — users and dashboard

**Files:**
- Create: `frontend/hooks/useAdminUsers.ts`
- Create: `frontend/hooks/useAdminDashboard.ts`

**Interfaces:**
- Consumes: `api`, `ApiEnvelope` (`@/lib/api`), `AdminUser`, `AdminDashboardStats` (Task 9).
- Produces: `useAdminUsers(params)`, `useAdminUser(id)`, `useDeactivateUser()`, `useReactivateUser()`, `useAdminDashboard()`.

- [ ] **Step 1: Write `useAdminUsers.ts`**

```typescript
"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { AdminUser } from "@/types/admin";

export const adminUserKeys = {
  all: ["admin", "users"] as const,
  list: (role?: string, active?: boolean) =>
    ["admin", "users", "list", role ?? null, active ?? null] as const,
  detail: (id: string) => ["admin", "users", id] as const,
};

export interface AdminUserFilters {
  role?: "TUTOR" | "STUDENT";
  active?: boolean;
}

async function fetchAdminUsers(filters: AdminUserFilters): Promise<AdminUser[]> {
  const res = await api.get<ApiEnvelope<AdminUser[]>>("/api/admin/users", { params: filters });
  return res.data.data;
}

async function fetchAdminUser(id: string): Promise<AdminUser> {
  const res = await api.get<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}`);
  return res.data.data;
}

export function useAdminUsers(filters: AdminUserFilters = {}) {
  return useQuery({
    queryKey: adminUserKeys.list(filters.role, filters.active),
    queryFn: () => fetchAdminUsers(filters),
  });
}

export function useAdminUser(id: string) {
  return useQuery({
    queryKey: adminUserKeys.detail(id),
    queryFn: () => fetchAdminUser(id),
    enabled: !!id,
  });
}

export function useDeactivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}/deactivate`);
      return res.data.data;
    },
    onSuccess: (user) => {
      queryClient.setQueryData(adminUserKeys.detail(id), user);
      queryClient.invalidateQueries({ queryKey: adminUserKeys.all });
    },
  });
}

export function useReactivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const res = await api.patch<ApiEnvelope<AdminUser>>(`/api/admin/users/${id}/reactivate`);
      return res.data.data;
    },
    onSuccess: (user) => {
      queryClient.setQueryData(adminUserKeys.detail(id), user);
      queryClient.invalidateQueries({ queryKey: adminUserKeys.all });
    },
  });
}
```

- [ ] **Step 2: Write `useAdminDashboard.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { AdminDashboardStats } from "@/types/admin";

export const adminDashboardKeys = {
  stats: ["admin", "dashboard"] as const,
};

async function fetchAdminDashboard(): Promise<AdminDashboardStats> {
  const res = await api.get<ApiEnvelope<AdminDashboardStats>>("/api/admin/dashboard");
  return res.data.data;
}

export function useAdminDashboard() {
  return useQuery({ queryKey: adminDashboardKeys.stats, queryFn: fetchAdminDashboard });
}
```

- [ ] **Step 3: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds (these hooks aren't imported by any page yet, so this only checks they type-check standalone).

- [ ] **Step 4: Commit**

```bash
git add frontend/hooks/useAdminUsers.ts frontend/hooks/useAdminDashboard.ts
git commit -m "Add admin users and dashboard React Query hooks"
```

---

## Task 12: Admin hooks — courses, assignments, submissions

**Files:**
- Create: `frontend/hooks/useAdminCourses.ts`
- Create: `frontend/hooks/useAdminAssignments.ts`
- Create: `frontend/hooks/useAdminSubmissions.ts`

**Interfaces:**
- Consumes: `api`, `ApiEnvelope`, `Course`, `Student`, `Assignment`, `Submission` (all existing types — DTO shapes are unchanged, reused as-is).
- Produces: `useAdminCourses()`, `useAdminCourse(id)`, `useAdminCourseStudents(id)`, `useAdminCourseAssignments(id)`, `useAdminAssignment(id)`, `useAdminAssignmentSubmissions(id)`, `useAdminSubmission(id)`.

- [ ] **Step 1: Write `useAdminCourses.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";
import type { Course } from "@/types/course";
import type { Student } from "@/types/user";

export const adminCourseKeys = {
  all: ["admin", "courses"] as const,
  detail: (id: string) => ["admin", "courses", id] as const,
  students: (id: string) => ["admin", "courses", id, "students"] as const,
  assignments: (id: string) => ["admin", "courses", id, "assignments"] as const,
};

async function fetchAdminCourses(): Promise<Course[]> {
  const res = await api.get<ApiEnvelope<Course[]>>("/api/admin/courses");
  return res.data.data;
}

async function fetchAdminCourse(id: string): Promise<Course> {
  const res = await api.get<ApiEnvelope<Course>>(`/api/admin/courses/${id}`);
  return res.data.data;
}

async function fetchAdminCourseStudents(id: string): Promise<Student[]> {
  const res = await api.get<ApiEnvelope<Student[]>>(`/api/admin/courses/${id}/students`);
  return res.data.data;
}

async function fetchAdminCourseAssignments(id: string): Promise<Assignment[]> {
  const res = await api.get<ApiEnvelope<Assignment[]>>(`/api/admin/courses/${id}/assignments`);
  return res.data.data;
}

export function useAdminCourses() {
  return useQuery({ queryKey: adminCourseKeys.all, queryFn: fetchAdminCourses });
}

export function useAdminCourse(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.detail(id),
    queryFn: () => fetchAdminCourse(id),
    enabled: !!id,
  });
}

export function useAdminCourseStudents(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.students(id),
    queryFn: () => fetchAdminCourseStudents(id),
    enabled: !!id,
  });
}

export function useAdminCourseAssignments(id: string) {
  return useQuery({
    queryKey: adminCourseKeys.assignments(id),
    queryFn: () => fetchAdminCourseAssignments(id),
    enabled: !!id,
  });
}
```

- [ ] **Step 2: Write `useAdminAssignments.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Assignment } from "@/types/assignment";
import type { Submission } from "@/types/submission";

export const adminAssignmentKeys = {
  detail: (id: string) => ["admin", "assignments", id] as const,
  submissions: (id: string) => ["admin", "assignments", id, "submissions"] as const,
};

async function fetchAdminAssignment(id: string): Promise<Assignment> {
  const res = await api.get<ApiEnvelope<Assignment>>(`/api/admin/assignments/${id}`);
  return res.data.data;
}

async function fetchAdminAssignmentSubmissions(id: string): Promise<Submission[]> {
  const res = await api.get<ApiEnvelope<Submission[]>>(`/api/admin/assignments/${id}/submissions`);
  return res.data.data;
}

export function useAdminAssignment(id: string) {
  return useQuery({
    queryKey: adminAssignmentKeys.detail(id),
    queryFn: () => fetchAdminAssignment(id),
    enabled: !!id,
  });
}

export function useAdminAssignmentSubmissions(id: string) {
  return useQuery({
    queryKey: adminAssignmentKeys.submissions(id),
    queryFn: () => fetchAdminAssignmentSubmissions(id),
    enabled: !!id,
  });
}
```

- [ ] **Step 3: Write `useAdminSubmissions.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type ApiEnvelope } from "@/lib/api";
import type { Submission } from "@/types/submission";

export const adminSubmissionKeys = {
  detail: (id: string) => ["admin", "submissions", id] as const,
};

async function fetchAdminSubmission(id: string): Promise<Submission> {
  const res = await api.get<ApiEnvelope<Submission>>(`/api/admin/submissions/${id}`);
  return res.data.data;
}

export function useAdminSubmission(id: string) {
  return useQuery({
    queryKey: adminSubmissionKeys.detail(id),
    queryFn: () => fetchAdminSubmission(id),
    enabled: !!id,
  });
}
```

- [ ] **Step 4: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds.

- [ ] **Step 5: Commit**

```bash
git add frontend/hooks/useAdminCourses.ts frontend/hooks/useAdminAssignments.ts frontend/hooks/useAdminSubmissions.ts
git commit -m "Add admin courses, assignments, and submissions React Query hooks"
```

---

## Task 13: Admin layout, sidebar wiring, and profile page

**Files:**
- Create: `frontend/app/admin/layout.tsx`
- Create: `frontend/app/admin/profile/page.tsx`

**Interfaces:**
- Consumes: `Sidebar` (`items` prop, existing, unchanged), `TopNav` (existing, unchanged), `useAuthGuard("ADMIN")` (Task 9), `ProfileView` (Task 10's fixed version).
- Produces: every subsequent admin page in Tasks 14–17 renders inside this layout.

- [ ] **Step 1: Write `app/admin/layout.tsx`**

```typescript
"use client";

import { IconBook2, IconLayoutDashboard, IconUsers } from "@tabler/icons-react";

import { Sidebar } from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";
import { useAuthGuard } from "@/hooks/useAuthGuard";

const navItems = [
  { href: "/admin/dashboard", label: "Dashboard", icon: IconLayoutDashboard },
  { href: "/admin/users", label: "Users", icon: IconUsers },
  { href: "/admin/courses", label: "Courses", icon: IconBook2 },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const authorized = useAuthGuard("ADMIN");
  if (!authorized) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar items={navItems} />
      <div className="flex flex-1 flex-col">
        <TopNav profileHref="/admin/profile" />
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Write `app/admin/profile/page.tsx`**

```typescript
import { PageHeader } from "@/components/layout/PageHeader";
import { ProfileView } from "@/components/user/ProfileView";

export default function AdminProfilePage() {
  return (
    <>
      <PageHeader title="Profile" description="View and edit your account details." />
      <ProfileView />
    </>
  );
}
```

- [ ] **Step 3: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; `/admin/dashboard`, `/admin/users`, `/admin/courses` don't exist as pages yet, so the nav links 404 until Tasks 14–16 — expected at this point, same as every prior phase's incremental build-up.

- [ ] **Step 4: Commit**

```bash
git add frontend/app/admin/layout.tsx frontend/app/admin/profile/page.tsx
git commit -m "Add admin layout, sidebar nav, and profile page"
```

---

## Task 14: Admin dashboard page

**Files:**
- Create: `frontend/app/admin/dashboard/page.tsx`

**Interfaces:**
- Consumes: `useAdminDashboard()` (Task 11).

- [ ] **Step 1: Write the dashboard page**

```typescript
"use client";

import { IconBook2, IconCircleCheck, IconClock, IconSchool, IconUsers } from "@tabler/icons-react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminDashboard } from "@/hooks/useAdminDashboard";

function StatCard({
  icon: Icon,
  label,
  value,
  isLoading,
}: {
  icon: typeof IconUsers;
  label: string;
  value: number | undefined;
  isLoading: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4">
        <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground">
          <Icon className="size-5" stroke={1.75} />
        </div>
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          {isLoading ? (
            <Skeleton className="mt-1 h-6 w-10" />
          ) : (
            <p className="text-xl font-medium text-foreground">{value ?? 0}</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export default function AdminDashboardPage() {
  const dashboard = useAdminDashboard();

  return (
    <>
      <PageHeader title="Dashboard" description="Platform-wide overview of every tutor and student." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard icon={IconSchool} label="Tutors" value={dashboard.data?.tutorCount} isLoading={dashboard.isLoading} />
        <StatCard icon={IconUsers} label="Students" value={dashboard.data?.studentCount} isLoading={dashboard.isLoading} />
        <StatCard icon={IconBook2} label="Courses" value={dashboard.data?.courseCount} isLoading={dashboard.isLoading} />
        <StatCard
          icon={IconClock}
          label="Submissions pending"
          value={dashboard.data?.submissionsPending}
          isLoading={dashboard.isLoading}
        />
        <StatCard
          icon={IconCircleCheck}
          label="Submissions released"
          value={dashboard.data?.submissionsReleased}
          isLoading={dashboard.isLoading}
        />
      </div>
    </>
  );
}
```

- [ ] **Step 2: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; `/admin/dashboard` now resolves.

- [ ] **Step 3: Commit**

```bash
git add frontend/app/admin/dashboard/page.tsx
git commit -m "Add admin dashboard page"
```

---

## Task 15: Admin users pages (list + detail with deactivate/reactivate)

**Files:**
- Create: `frontend/app/admin/users/page.tsx`
- Create: `frontend/app/admin/users/[id]/page.tsx`

**Interfaces:**
- Consumes: `useAdminUsers`, `useAdminUser`, `useDeactivateUser`, `useReactivateUser` (Task 11).

- [ ] **Step 1: Write the users list page**

```typescript
"use client";

import Link from "next/link";

import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAdminUsers } from "@/hooks/useAdminUsers";

export default function AdminUsersPage() {
  const users = useAdminUsers();

  return (
    <>
      <PageHeader title="Users" description="Every tutor and student on the platform." />

      {users.isLoading ? (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : users.data && users.data.length > 0 ? (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Joined</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.data.map((user) => (
              <TableRow key={user.id}>
                <TableCell>
                  <Link href={`/admin/users/${user.id}`} className="font-medium text-foreground">
                    {user.name}
                  </Link>
                </TableCell>
                <TableCell className="text-muted-foreground">{user.email}</TableCell>
                <TableCell>
                  <Badge variant="outline">{user.role === "TUTOR" ? "Tutor" : "Student"}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={user.active ? "secondary" : "destructive"}>
                    {user.active ? "Active" : "Deactivated"}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {new Date(user.createdAt).toLocaleDateString()}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      ) : (
        <p className="text-sm text-muted-foreground">No users yet.</p>
      )}
    </>
  );
}
```

- [ ] **Step 2: Write the user detail page**

```typescript
"use client";

import { use } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminUser, useDeactivateUser, useReactivateUser } from "@/hooks/useAdminUsers";

export default function AdminUserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const user = useAdminUser(id);
  const deactivate = useDeactivateUser(id);
  const reactivate = useReactivateUser(id);

  if (user.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!user.data) {
    return <p className="text-sm text-muted-foreground">User not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={user.data.name}
        description={user.data.email}
        actions={
          user.data.active ? (
            <Button
              variant="outline"
              disabled={deactivate.isPending}
              onClick={() => deactivate.mutate()}
            >
              {deactivate.isPending ? "Deactivating..." : "Deactivate"}
            </Button>
          ) : (
            <Button
              variant="outline"
              disabled={reactivate.isPending}
              onClick={() => reactivate.mutate()}
            >
              {reactivate.isPending ? "Reactivating..." : "Reactivate"}
            </Button>
          )
        }
      />

      <Card>
        <CardContent className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Badge variant="outline">{user.data.role === "TUTOR" ? "Tutor" : "Student"}</Badge>
            <Badge variant={user.data.active ? "secondary" : "destructive"}>
              {user.data.active ? "Active" : "Deactivated"}
            </Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            Joined {new Date(user.data.createdAt).toLocaleDateString()}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; `/admin/users` and `/admin/users/[id]` now resolve.

- [ ] **Step 4: Commit**

```bash
git add frontend/app/admin/users/page.tsx "frontend/app/admin/users/[id]/page.tsx"
git commit -m "Add admin users list and detail pages with deactivate/reactivate"
```

---

## Task 16: Admin courses pages (list + detail with roster/assignments)

**Files:**
- Create: `frontend/app/admin/courses/page.tsx`
- Create: `frontend/app/admin/courses/[id]/page.tsx`

**Interfaces:**
- Consumes: `useAdminCourses`, `useAdminCourse`, `useAdminCourseStudents`, `useAdminCourseAssignments` (Task 12), `StudentRow` with its new `href` prop (Task 10).

- [ ] **Step 1: Write the courses list page**

```typescript
"use client";

import Link from "next/link";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminCourses } from "@/hooks/useAdminCourses";

export default function AdminCoursesPage() {
  const courses = useAdminCourses();

  return (
    <>
      <PageHeader title="Courses" description="Every course across every tutor." />

      {courses.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : courses.data && courses.data.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {courses.data.map((course) => (
            <Link key={course.id} href={`/admin/courses/${course.id}`}>
              <Card className="transition-colors hover:bg-muted/40">
                <CardContent>
                  <p className="font-medium text-foreground">{course.name}</p>
                  <p className="text-sm text-muted-foreground">{course.subject}</p>
                  <p className="mt-1 text-xs text-muted-foreground">Taught by {course.tutorName}</p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">No courses yet.</p>
      )}
    </>
  );
}
```

- [ ] **Step 2: Write the course detail page**

```typescript
"use client";

import { use } from "react";
import Link from "next/link";

import { StudentRow } from "@/components/student/StudentRow";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  useAdminCourse,
  useAdminCourseAssignments,
  useAdminCourseStudents,
} from "@/hooks/useAdminCourses";

export default function AdminCourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const course = useAdminCourse(id);
  const students = useAdminCourseStudents(id);
  const assignments = useAdminCourseAssignments(id);

  if (course.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!course.data) {
    return <p className="text-sm text-muted-foreground">Course not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-medium text-foreground">{course.data.name}</h1>
        <p className="text-sm text-muted-foreground">
          {course.data.subject} · Taught by {course.data.tutorName}
        </p>
        {course.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{course.data.description}</p>
        ) : null}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Enrolled students</h2>
        {students.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : students.data && students.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Added</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {students.data.map((student) => (
                <StudentRow key={student.id} student={student} href={`/admin/users/${student.id}`} />
              ))}
            </TableBody>
          </Table>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No students enrolled yet.</p>
            </CardContent>
          </Card>
        )}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Assignments</h2>
        {assignments.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : assignments.data && assignments.data.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
            {assignments.data.map((assignment) => (
              <Link key={assignment.id} href={`/admin/assignments/${assignment.id}`}>
                <Card className="transition-colors hover:bg-muted/40">
                  <CardContent>
                    <p className="font-medium text-foreground">{assignment.title}</p>
                    <p className="text-sm text-muted-foreground">
                      Due {new Date(assignment.dueDate).toLocaleDateString()}
                    </p>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No assignments yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; `/admin/courses` and `/admin/courses/[id]` now resolve, and clicking a roster row navigates to `/admin/users/[id]`, not `/students/[id]`.

- [ ] **Step 4: Commit**

```bash
git add frontend/app/admin/courses/page.tsx "frontend/app/admin/courses/[id]/page.tsx"
git commit -m "Add admin courses list and detail pages with roster and assignments"
```

---

## Task 17: Admin assignment + submission detail pages

**Files:**
- Create: `frontend/app/admin/assignments/[id]/page.tsx`
- Create: `frontend/components/admin/AdminSubmissionDetail.tsx`
- Create: `frontend/app/admin/submissions/[id]/page.tsx`

**Interfaces:**
- Consumes: `useAdminAssignment`, `useAdminAssignmentSubmissions` (Task 12), `useAdminSubmission` (Task 12), `SageFeedbackBlock` (existing, read-only, unchanged).
- Produces: `AdminSubmissionDetail` — a read-only submission view with **no mutation actions**, deliberately not reusing `SubmissionReview` (which calls tutor-only endpoints admin isn't authorized for, and includes edit actions out of scope per the spec).

- [ ] **Step 1: Write the admin assignment detail page**

```typescript
"use client";

import { use } from "react";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAdminAssignment, useAdminAssignmentSubmissions } from "@/hooks/useAdminAssignments";
import type { FeedbackStatus } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

export default function AdminAssignmentDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const assignment = useAdminAssignment(id);
  const submissions = useAdminAssignmentSubmissions(id);

  if (assignment.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (!assignment.data) {
    return <p className="text-sm text-muted-foreground">Assignment not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-medium text-foreground">{assignment.data.title}</h1>
        <p className="text-sm text-muted-foreground">
          {assignment.data.courseName} · Due {new Date(assignment.data.dueDate).toLocaleDateString()}
        </p>
        {assignment.data.description ? (
          <p className="mt-2 text-sm text-muted-foreground">{assignment.data.description}</p>
        ) : null}
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Submissions</h2>
        {submissions.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : submissions.data && submissions.data.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Submitted</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Grade</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {submissions.data.map((submission) => (
                <TableRow key={submission.id}>
                  <TableCell>
                    <Link
                      href={`/admin/submissions/${submission.id}`}
                      className="font-medium text-foreground"
                    >
                      {submission.studentName}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {new Date(submission.submittedAt).toLocaleString()}
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusVariant[submission.feedbackStatus]}>
                      {submission.feedbackStatus}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{submission.grade ?? "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : (
          <Card>
            <CardContent>
              <p className="text-sm text-muted-foreground">No submissions yet.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Write the read-only `AdminSubmissionDetail` component**

```typescript
import { SageFeedbackBlock } from "@/components/sage/SageFeedbackBlock";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { FeedbackStatus, Submission } from "@/types/submission";

const statusVariant: Record<FeedbackStatus, "default" | "secondary"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REVISED: "default",
};

interface AdminSubmissionDetailProps {
  submission: Submission;
}

export function AdminSubmissionDetail({ submission }: AdminSubmissionDetailProps) {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-muted-foreground">Submitted by</p>
          <p className="font-medium text-foreground">{submission.studentName}</p>
        </div>
        <Badge variant={statusVariant[submission.feedbackStatus]}>{submission.feedbackStatus}</Badge>
      </div>

      <div className="flex flex-col gap-1.5">
        <p className="text-sm font-medium text-foreground">Submitted code</p>
        <pre className="overflow-x-auto rounded-card border-hairline border-border bg-muted/40 p-4 font-mono text-xs text-foreground">
          {submission.content}
        </pre>
      </div>

      {submission.rubricScores.length > 0 ? (
        <Card>
          <CardContent className="flex flex-col gap-2">
            <p className="text-sm font-medium text-foreground">Rubric scores</p>
            {submission.rubricScores.map((score) => (
              <div key={score.criterionId} className="flex items-center justify-between">
                <p className="text-sm text-foreground">{score.criterionName}</p>
                <p className="text-sm text-muted-foreground">
                  {score.pointsAwarded} / {score.pointsPossible}
                </p>
              </div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      {submission.sageFeedback ? <SageFeedbackBlock feedbackJson={submission.sageFeedback} /> : null}

      {submission.tutorFeedback ? (
        <div className="flex flex-col gap-1.5">
          <p className="text-sm font-medium text-foreground">Tutor feedback</p>
          <p className="text-sm text-muted-foreground">{submission.tutorFeedback}</p>
        </div>
      ) : null}

      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>Grade: {submission.grade ?? "—"}</span>
        <span>·</span>
        <span>
          {submission.releasedAt
            ? `Released ${new Date(submission.releasedAt).toLocaleString()}`
            : "Not yet released to student"}
        </span>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Write the admin submission detail page**

```typescript
"use client";

import { use } from "react";

import { AdminSubmissionDetail } from "@/components/admin/AdminSubmissionDetail";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminSubmission } from "@/hooks/useAdminSubmissions";

export default function AdminSubmissionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const submission = useAdminSubmission(id);

  if (submission.isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!submission.data) {
    return <p className="text-sm text-muted-foreground">Submission not found.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-medium text-foreground">Submission</h1>
      <AdminSubmissionDetail submission={submission.data} />
    </div>
  );
}
```

- [ ] **Step 4: Verify it builds**

Run: `npm run build` (in `frontend/`)
Expected: build succeeds; all 8 new admin routes now exist (`/admin/dashboard`, `/admin/users`, `/admin/users/[id]`, `/admin/courses`, `/admin/courses/[id]`, `/admin/assignments/[id]`, `/admin/submissions/[id]`, `/admin/profile`).

- [ ] **Step 5: Commit**

```bash
git add "frontend/app/admin/assignments/[id]/page.tsx" frontend/components/admin/AdminSubmissionDetail.tsx "frontend/app/admin/submissions/[id]/page.tsx"
git commit -m "Add admin assignment and read-only submission detail pages"
```

---

## Task 18: Final verification pass

**Files:** none (verification only)

- [ ] **Step 1: Full backend compile**

Run: `./mvnw compile`
Expected: `BUILD SUCCESS` across all classes, including the new `com.notare.admin` package.

- [ ] **Step 2: Full frontend build + lint**

Run: `npm run build` (in `frontend/`)
Run: `npm run lint` (in `frontend/`)
Expected: build succeeds for all routes (existing 14 + the 8 new admin ones); lint shows only the pre-existing accepted `no-location-assign-relative-destination` warning in `lib/api.ts`, no new warnings.

- [ ] **Step 3: Live dev-server smoke test**

With the frontend dev server running (`npm run dev`), fetch each new route and confirm it 200s and renders its shell (data itself will show empty/loading state without a running backend, which is expected — same standing limitation as every prior frontend phase):
- `/admin/dashboard`
- `/admin/users`
- `/admin/users/[id]` (any UUID — expect "User not found" body, not a crash)
- `/admin/courses`
- `/admin/courses/[id]`
- `/admin/assignments/[id]`
- `/admin/submissions/[id]`
- `/admin/profile`

Also confirm `/dashboard` (tutor) and `/student/dashboard` still render correctly — the `useAuthGuard` and login-redirect changes in Task 9 touched shared code paths.

- [ ] **Step 4: Update CLAUDE.md and CHANGELOG.md**

Per the project's standing rule ("After any non-trivial change... add a dated entry to CHANGELOG.md"), add an entry summarizing the admin portal addition: the new `ADMIN` role, the parallel `/api/admin/*` + `/admin/*` surface, the registration-hardening fix, and the soft-deactivate-only decision (with the dropped hard-delete/cascade-migration path noted as a considered-and-rejected alternative). Also add a line to CLAUDE.md's "Progress" section noting the admin portal as a post-launch addition beyond the original 13-phase build order, consistent with how the CI/CD section is already tracked separately from the phase list.

- [ ] **Step 5: Final commit**

```bash
git add CLAUDE.md CHANGELOG.md
git commit -m "Document the admin portal addition in CLAUDE.md and CHANGELOG"
```
