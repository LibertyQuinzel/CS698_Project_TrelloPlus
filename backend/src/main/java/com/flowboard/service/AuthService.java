package com.flowboard.service;

import com.flowboard.dto.*;
import com.flowboard.entity.User;
import com.flowboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final SecurityQuestionService securityQuestionService;
    private final ConcurrentMap<String, PasswordResetSession> resetSessions = new ConcurrentHashMap<>();

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (activeUserExistsByEmail(email, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        validatePasswordBusinessRules(request.getPassword(), email, request.getFullName());

        String username = resolveUsername(request);

        User user = User.builder()
            .email(email)
            .username(username)
            .fullName(request.getFullName().trim())
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
            rawUsername = request.getEmail() != null ? normalizeEmail(request.getEmail()).split("@")[0] : "user";
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
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
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
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDTO getUserProfile(String userId) {
        User user = getUserById(userId);
        return toUserDTO(user);
    }

    public UserDTO updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = getUserById(userId);
        
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            // Check if email is already taken by another user
            if (activeUserExistsByEmail(normalizedEmail, user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(normalizedEmail);
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

    public void logout(String token) {
        jwtService.revokeToken(token);
    }

    public void setSecurityQuestions(String userId, SetSecurityQuestionsRequest request) {
        User user = getUserById(userId);
        boolean updatingExistingQuestions = hasConfiguredSecurityQuestions(user);

        if (updatingExistingQuestions) {
            if (isBlank(request.getCurrentPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
            }
        }

        requireQuestionAndAnswer(request.getSecurityQuestion1(), request.getSecurityAnswer1(), "Security question 1 and answer are required");
        requireQuestionAndAnswer(request.getSecurityQuestion2(), request.getSecurityAnswer2(), "Security question 2 and answer are required");
        requireQuestionAndAnswer(request.getCustomSecurityQuestion(), request.getCustomSecurityAnswer(), "Custom security question and answer are required");

        validateAnswerLength(request.getSecurityAnswer1());
        validateAnswerLength(request.getSecurityAnswer2());
        validateAnswerLength(request.getCustomSecurityAnswer());

        user.setSecurityQuestion1(request.getSecurityQuestion1().trim());
        user.setSecurityAnswer1Hash(passwordEncoder.encode(securityQuestionService.normalizeAnswer(request.getSecurityAnswer1())));
        user.setSecurityQuestion2(request.getSecurityQuestion2().trim());
        user.setSecurityAnswer2Hash(passwordEncoder.encode(securityQuestionService.normalizeAnswer(request.getSecurityAnswer2())));
        user.setCustomSecurityQuestion(request.getCustomSecurityQuestion().trim());
        user.setCustomSecurityAnswerHash(passwordEncoder.encode(securityQuestionService.normalizeAnswer(request.getCustomSecurityAnswer())));
        user.setFailedSecurityAttempts(0);
        user.setLastSecurityAttemptTime(null);
        userRepository.save(user);
    }

    public SecurityQuestionsResponse getMySecurityQuestions(String userId) {
        User user = getUserById(userId);
        return toSecurityQuestionsResponse(user);
    }

    public SecurityQuestionsResponse getSecurityQuestions(String email) {
        User user = findNewestByEmail(email);
        return toSecurityQuestionsResponse(user);
    }

    public PasswordResetTokenResponse validateSecurityAnswers(ValidateSecurityAnswersRequest request) {
        User user = findNewestByEmail(request.getEmail());

        if (!hasConfiguredSecurityQuestions(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security questions are not configured");
        }

        int failedAttempts = user.getFailedSecurityAttempts() == null ? 0 : user.getFailedSecurityAttempts();
        if (securityQuestionService.isLockedOut(failedAttempts, user.getLastSecurityAttemptTime())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please try again later.");
        }

        ValidateSecurityAnswersRequest.SecurityAnswers answers = request.getAnswers();
        boolean matches = passwordEncoder.matches(securityQuestionService.normalizeAnswer(answers.getAnswer1()), user.getSecurityAnswer1Hash())
            && passwordEncoder.matches(securityQuestionService.normalizeAnswer(answers.getAnswer2()), user.getSecurityAnswer2Hash())
            && passwordEncoder.matches(securityQuestionService.normalizeAnswer(answers.getCustomAnswer()), user.getCustomSecurityAnswerHash());

        if (!matches) {
            user.setFailedSecurityAttempts(failedAttempts + 1);
            user.setLastSecurityAttemptTime(LocalDateTime.now());
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Security answers did not match");
        }

        user.setFailedSecurityAttempts(0);
        user.setLastSecurityAttemptTime(null);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        resetSessions.put(token, new PasswordResetSession(user.getId(), securityQuestionService.resetTokenExpiresAt()));

        return PasswordResetTokenResponse.builder()
            .resetToken(token)
            .message("Security questions verified. You can now reset your password.")
            .build();
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetSession session = resetSessions.get(request.getResetToken());
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            resetSessions.remove(request.getResetToken());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is invalid or expired");
        }

        User user = userRepository.findById(session.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        validatePasswordBusinessRules(request.getNewPassword(), user.getEmail(), user.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetSessions.remove(request.getResetToken());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void validatePasswordBusinessRules(String password, String email, String fullName) {
        String passwordLower = password.toLowerCase();
        if (email != null && passwordLower.contains(email.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not contain email");
        }

        if (fullName != null) {
            String nameToken = fullName.trim().toLowerCase().replace(" ", "");
            if (!nameToken.isEmpty() && passwordLower.contains(nameToken)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not contain full name");
            }
        }
    }

    private User findNewestByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(normalizeEmail(email)).stream()
            .filter(this::isActiveUser)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean activeUserExistsByEmail(String email, UUID excludedUserId) {
        return userRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
            .filter(this::isActiveUser)
            .anyMatch(user -> excludedUserId == null || !excludedUserId.equals(user.getId()));
    }

    private boolean isActiveUser(User user) {
        return user.getIsDeletionMarked() == null || !user.getIsDeletionMarked();
    }

    private SecurityQuestionsResponse toSecurityQuestionsResponse(User user) {
        if (!hasConfiguredSecurityQuestions(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security questions are not configured");
        }

        return SecurityQuestionsResponse.builder()
            .question1(user.getSecurityQuestion1())
            .question2(user.getSecurityQuestion2())
            .customQuestion(user.getCustomSecurityQuestion())
            .build();
    }

    private boolean hasConfiguredSecurityQuestions(User user) {
        return !isBlank(user.getSecurityQuestion1())
            && !isBlank(user.getSecurityAnswer1Hash())
            && !isBlank(user.getSecurityQuestion2())
            && !isBlank(user.getSecurityAnswer2Hash())
            && !isBlank(user.getCustomSecurityQuestion())
            && !isBlank(user.getCustomSecurityAnswerHash());
    }

    private void requireQuestionAndAnswer(String question, String answer, String message) {
        if (isBlank(question) || isBlank(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateAnswerLength(String answer) {
        if (answer != null && answer.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security answers must be 500 characters or fewer");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PasswordResetSession(UUID userId, LocalDateTime expiresAt) {}
}
