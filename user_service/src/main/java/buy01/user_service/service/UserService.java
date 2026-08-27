package buy01.user_service.service;

import buy01.user_service.dto.ProfileRequest;
import buy01.user_service.dto.ProfileResponse;
import buy01.user_service.event.UserDeletedEvent;
import buy01.user_service.exceptions.BadRequestException;
import buy01.user_service.model.Role;
import buy01.user_service.model.User;
import buy01.user_service.producer.UserEventProducer;
import buy01.user_service.repo.UserRepository;
import buy01.user_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mongodb.DuplicateKeyException;

@Service
@RequiredArgsConstructor
public class UserService {
    // communicate
    private final UserEventProducer producer;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> register(String username, String email, String password, String role, String avatarUrl) {
        // 1. Sanitize inputs
        String cleanUsername = username != null ? username.trim() : "";
        String cleanEmail = email != null ? email.toLowerCase().trim() : "";
        String cleanPassword = password != null ? password.trim() : "";
        String cleanRole = role != null ? role.trim().toUpperCase() : "CLIENT";
        String cleanAvatarUrl = avatarUrl != null ? avatarUrl.trim() : "";

        // 2. Manual regex validation check
        if (!cleanUsername.matches("^[a-zA-Z0-9]{3,20}$")) {
            throw new BadRequestException(
                    "Username must be between 3 and 20 characters and contain only letters and numbers");
        }
        if (!cleanEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("Invalid email format");
        }
        if (!cleanPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{4,}$")) {
            throw new BadRequestException(
                    "Password must be at least 4 characters long and contain at least one letter and one number");
        }

        // 3. Uniqueness verification
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Email already exists");
        } else if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new BadRequestException("Username already exists");
        }

        // 4. Role parsing
        Role checkedRole = "SELLER".equals(cleanRole) ? Role.SELLER : Role.CLIENT;

        // 5. Entity creation
        User user = User.builder()
                .username(cleanUsername)
                .email(cleanEmail)
                .password(passwordEncoder.encode(cleanPassword))
                .role(checkedRole)
                .createdAt(LocalDateTime.now().toString())
                .avatarUrl(cleanAvatarUrl)
                .build();

        // 6. Persistence
        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("Email or username already exists");
        }

        // 7. Response mapping
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully");

        return response;
    }

    public Map<String, Object> login(String email, String password) {
        System.out.println("Login attempt for email: " + email);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new BadRequestException("User not found");
        }
        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            throw new BadRequestException("Invalid password");
        }
        String token = jwtUtil.generateToken(user.get());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("token", token);
        return response;
    }

    @Cacheable(value = "profiles", key = "#userId")
    public ProfileResponse getProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getCreatedAt());
    }

    @CachePut(value = "profiles", key = "#userId")
    public ProfileResponse updateProfile(String userId, ProfileRequest profile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Null checks & Sanitization
        if (profile.getUsername() == null || profile.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username cannot be empty");
        }
        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email cannot be empty");
        }
        if (profile.getRole() == null) {
            throw new BadRequestException("Role cannot be null");
        }

        String cleanUsername = profile.getUsername().trim();
        String cleanEmail = profile.getEmail().toLowerCase().trim();
        String cleanAvatarUrl = profile.getAvatarUrl() != null ? profile.getAvatarUrl().trim() : "";

        // 2. Pattern Validations
        if (!cleanUsername.matches("^[a-zA-Z0-9]{3,20}$")) {
            throw new BadRequestException(
                    "Username must be between 3 and 20 characters and contain only letters and numbers");
        }
        if (!cleanEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("Invalid email format");
        }

        // 3. Enum Role Check
        if (profile.getRole() != Role.CLIENT && profile.getRole() != Role.SELLER) {
            throw new BadRequestException("Invalid role. Must be CLIENT or SELLER");
        }

        // 4. Uniqueness checks (with sanitized values)
        userRepository.findByUsername(cleanUsername).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new BadRequestException("Username already taken by another account");
            }
        });

        userRepository.findByEmail(cleanEmail).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new BadRequestException("Email address already registered by another account");
            }
        });

        // 5. Update Entity
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setAvatarUrl(cleanAvatarUrl);
        user.setRole(profile.getRole());

        // 6. Persistence & Cache Return
        User updatedUser = userRepository.save(user);

        return new ProfileResponse(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getAvatarUrl(),
                updatedUser.getCreatedAt());
    }

    @CacheEvict(value = "profiles", key = "#userId")
    public Map<String, Object> deleteProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
        // Send UserDeletedEvent to Kafka
        producer.sendUserDeletedEvent(new UserDeletedEvent(userId));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User deleted successfully");

        return response;
    }
}