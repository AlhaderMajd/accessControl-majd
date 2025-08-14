package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.EmailAlreadyUsedException;
import com.example.accesscontrol.exception.InvalidCredentialsException;
import com.example.accesscontrol.exception.UserDisabledException;
import com.example.accesscontrol.exception.UserNotFoundException;
import com.example.accesscontrol.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        final String email = request.getEmail() == null ? null : request.getEmail().trim();

        if (email == null || request.getPassword() == null || !isValidEmail(email)) {
            auditLoginFailure(email, "invalid_format");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user;
        try {
            user = userService.getByEmailOrThrow(email);
        } catch (UserNotFoundException ex) {
            passwordEncoder.matches("dummy-password",
                    "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiD7HkGKuGJySLjeRGna43EIBgzHuMG");
            auditLoginFailure(email, "not_found");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLoginFailure(email, "bad_password");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            auditLoginFailure(email, "disabled");
            throw new UserDisabledException();
        }

        List<String> roles = userRoleService.getRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) {
            auditLoginFailure(email, "no_roles");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        auditLoginSuccess(user.getId(), email);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .roles(roles)
                .build();
    }

    private void auditLoginFailure(String email, String reason) {
        log.info("auth.login.failed email={} reason={}", mask(email), reason);
    }

    private void auditLoginSuccess(Long userId, String email) {
        log.info("auth.login.success userId={} email={}", userId, mask(email));
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        final String email = request.getEmail() == null ? null : request.getEmail().trim();
        final String rawPassword = request.getPassword();

        if (email == null || rawPassword == null || !isValidEmail(email) || !isValidPassword(rawPassword)) {
            auditRegisterFailure(email, "invalid_format");
            throw new InvalidCredentialsException("Invalid email or password format");
        }

        if (userService.emailExists(email)) {
            auditRegisterFailure(email, "email_in_use_precheck");
            throw new EmailAlreadyUsedException("Email already in use");
        }

        try {
            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .enabled(false)
                    .build();

            User saved = userService.save(newUser);

            Role member = roleService.getOrCreateRole("MEMBER");
            userRoleService.assignRolesToUsers(List.of(saved.getId()), List.of(member.getId()));

            auditRegisterSuccess(saved.getId(), email);

            return AuthResponse.builder()
                    .token(null)
                    .userId(saved.getId())
                    .roles(List.of(member.getName()))
                    .build();

        } catch (DataIntegrityViolationException ex) {
            auditRegisterFailure(email, "email_in_use_violation");
            throw new EmailAlreadyUsedException("Email already in use");
        }
    }

    private void auditRegisterSuccess(Long userId, String email) {
        log.info("auth.register.success userId={} email={}", userId, mask(email));
    }

    private void auditRegisterFailure(String email, String reason) {
        log.info("auth.register.failed email={} reason={}", mask(email), reason);
    }

    private String mask(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return "unknown";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String head = local.isEmpty() ? "*" : local.substring(0, 1);
        return head + "***@" + domain;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
