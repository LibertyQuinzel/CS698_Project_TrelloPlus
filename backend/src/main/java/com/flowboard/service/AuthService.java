package com.flowboard.service;

import com.flowboard.dto.*;
import com.flowboard.entity.User;
import com.flowboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String username = resolveUsername(request);

        User user = User.builder()
            .email(request.getEmail())
            .username(username)
            .fullName(request.getFullName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(User.UserRole.MEMBER)
            .build();

        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().toString());

        return AuthResponse.builder()
            .token(token)
            .expiresIn(jwtService.getExpirationTime(token))
            .user(toUserDTO(user))
            .build();
    }

    private String resolveUsername(RegisterRequest request) {
        String rawUsername = request.getUsername();

        if (rawUsername == null || rawUsername.isBlank()) {
            rawUsername = request.getFullName();
        }

        if (rawUsername == null || rawUsername.isBlank()) {
            rawUsername = request.getEmail() != null ? request.getEmail().split("@")[0] : "user";
        }

        String base = rawUsername
            .trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");

        if (base.isBlank()) {
            base = "user";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + suffix;
            suffix++;
        }

        return candidate;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().toString());

        return AuthResponse.builder()
            .token(token)
            .expiresIn(jwtService.getExpirationTime(token))
            .user(toUserDTO(user))
            .build();
    }

    public User getUserById(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserDTO getUserProfile(String userId) {
        User user = getUserById(userId);
        return toUserDTO(user);
    }

    public UserDTO updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = getUserById(userId);
        
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        
        user = userRepository.save(user);
        return toUserDTO(user);
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .role(user.getRole().toString())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
