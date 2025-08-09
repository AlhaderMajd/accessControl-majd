package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.*;
import com.example.accesscontrol.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest request) {
        User user = userService.getByEmailOrThrow(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new InvalidCredentialsException();
        if (!user.isEnabled()) throw new UserDisabledException();
        List<String> roles = userRoleService.getRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) throw new RuntimeException("User has no assigned roles");
        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .roles(roles)
                .build();
    }

    public AuthResponse register(AuthRequest request) {
        if (!isValidEmail(request.getEmail()) || !isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password format");
        }
        if (userService.emailExists(request.getEmail())) {
            throw new EmailAlreadyUsedException("Email already in use");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEnabled(true);

        User savedUser = userService.save(newUser);

        Role memberRole = roleService.getOrCreateRole("MEMBER");
        userRoleService.assignRolesToUsers(List.of(savedUser.getId()), List.of(memberRole.getId()));

        String token = jwtTokenProvider.generateToken(savedUser.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .roles(List.of(memberRole.getName()))
                .build();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
