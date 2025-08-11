package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.EmailAlreadyUsedException;
import com.example.accesscontrol.exception.InvalidCredentialsException;
import com.example.accesscontrol.exception.UserDisabledException;
import com.example.accesscontrol.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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
        User user = userService.getByEmailOrThrow(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            throw new UserDisabledException();
        }

        List<String> roles = userRoleService.getRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new InvalidCredentialsException("User has no assigned roles");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .roles(roles)
                .build();
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (!isValidEmail(request.getEmail()) || !isValidPassword(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password format");
        }
        if (userService.emailExists(request.getEmail())) {
            throw new EmailAlreadyUsedException("Email already in use");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .build();
        User saved = userService.save(newUser);

        Role member = roleService.getOrCreateRole("MEMBER");
        userRoleService.assignRolesToUsers(List.of(saved.getId()), List.of(member.getId()));

        String token = jwtTokenProvider.generateToken(saved.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(saved.getId())
                .roles(List.of(member.getName()))
                .build();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
