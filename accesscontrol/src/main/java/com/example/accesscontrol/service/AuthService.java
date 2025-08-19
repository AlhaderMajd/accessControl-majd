package com.example.accesscontrol.service;

import com.example.accesscontrol.config.logs;
import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.LoginAuthResponse;
import com.example.accesscontrol.dto.auth.RegisterAuthResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Invalid email or password";
    private static final String DUMMY_BCRYPT =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiD7HkGKuGJySLjeRGna43EIBgzHuMG";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,24}$"
    );

    private final UserService userService;
    private final RoleService roleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final logs logs;

    @Transactional(readOnly = true)
    public LoginAuthResponse login(AuthRequest request) {
        final String email = request.getEmail() == null ? null : request.getEmail().strip();
        final String password = request.getPassword();

        if (isInvalidPassword(password) || isInvalidEmail(email)) {
            throw deny(email, "invalid_format");
        }

        final User user;
        try {
            user = userService.getByEmailOrThrow(email);
        } catch (UserNotFoundException ex) {
            passwordEncoder.matches("dummy-password", DUMMY_BCRYPT);
            throw deny(email, "not_found");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw deny(email, "bad_password");
        }

        if (!user.isEnabled()) {
            auditLoginFailure(email, "disabled");
            throw new UserDisabledException();
        }

        final List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        if (roles.isEmpty()) {
            throw deny(email, "no_roles");
        }

        final String token = jwtTokenProvider.generateToken(user.getEmail());
        auditLoginSuccess(user.getId(), email);

        return LoginAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .roles(roles)
                .build();
    }

    @Transactional
    public RegisterAuthResponse register(AuthRequest request) {
        final String email = request.getEmail() == null ? null : request.getEmail().strip();
        final String rawPassword = request.getPassword();

        if (isInvalidEmail(email) || isInvalidPassword(rawPassword)) {
            throw deny(email, "invalid_format");
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

            if (saved.getRoles() == null) {
                saved.setRoles(new LinkedHashSet<>());
            }
            saved.getRoles().add(member);

            userService.save(saved);

            auditRegisterSuccess(saved.getId(), email);

            return RegisterAuthResponse.builder()
                    .userId(saved.getId())
                    .roles(List.of(member.getName()))
                    .build();

        } catch (DataIntegrityViolationException ex) {
            auditRegisterFailure(email, "email_in_use_violation");
            throw new EmailAlreadyUsedException("Email already in use");
        }
    }

    private InvalidCredentialsException deny(String email, String reason) {
        auditLoginFailure(email, reason);
        return new InvalidCredentialsException(INVALID_CREDENTIALS_MSG);
    }

    private boolean isInvalidEmail(String email) {
        return email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isInvalidPassword(String password) {
        return password == null || password.length() < 6;
    }

    private void auditLoginFailure(String email, String reason) {
        log.info("auth.login.failed email={} reason={}", logs.mask(email), reason);
    }

    private void auditLoginSuccess(Long userId, String email) {
        log.info("auth.login.success userId={} email={}", userId, logs.mask(email));
    }

    private void auditRegisterSuccess(Long userId, String email) {
        log.info("auth.register.success userId={} email={}", userId, logs.mask(email));
    }

    private void auditRegisterFailure(String email, String reason) {
        log.info("auth.register.failed email={} reason={}", logs.mask(email), reason);
    }
}
