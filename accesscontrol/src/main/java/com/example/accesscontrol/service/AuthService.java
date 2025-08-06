package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.AuthRequest;
import com.example.accesscontrol.dto.AuthResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.entity.UserRole;
import com.example.accesscontrol.exception.*;
import com.example.accesscontrol.repository.RoleRepository;
import com.example.accesscontrol.repository.UserRepository;
import com.example.accesscontrol.repository.UserRoleRepository;
import com.example.accesscontrol.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new UserDisabledException();
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());

        if (userRoles.isEmpty()) {
            throw new RuntimeException("User has no assigned roles");
        }

        List<String> roles = userRoles.stream()
                .map(userRole -> roleRepository.findById(userRole.getRoleId())
                        .orElseThrow(() -> new RuntimeException("Role not found")))
                .map(Role::getName)
                .toList();


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

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException("Email already in use");
        }

        // 1. Create and save user
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEnabled(true);

        User savedUser = userRepository.save(newUser);

        // 2. Assign MEMBER role using userRoleService
        Role savedRole = userRoleService.assignRoleToUser(savedUser.getId(), "MEMBER");

        // 3. Generate JWT token
        String token = jwtTokenProvider.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .roles(List.of(savedRole.getName()))
                .build();
    }


    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}