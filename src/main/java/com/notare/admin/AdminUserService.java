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
