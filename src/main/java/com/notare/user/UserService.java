package com.notare.user;

import com.notare.user.dto.UpdateProfileRequest;
import com.notare.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(requireUser(email));
    }

    @Transactional
    public UserResponse updateCurrentUser(String email, UpdateProfileRequest request) {
        User user = requireUser(email);
        user.setName(request.name());
        return UserResponse.from(user);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
