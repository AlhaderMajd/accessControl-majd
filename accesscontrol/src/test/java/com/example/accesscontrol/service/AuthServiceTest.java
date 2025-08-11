package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.EmailAlreadyUsedException;
import com.example.accesscontrol.exception.InvalidCredentialsException;
import com.example.accesscontrol.exception.UserDisabledException;
import com.example.accesscontrol.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private UserRoleService userRoleService;
    @Mock private RoleService roleService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User enabledUser;

    @BeforeEach
    void setup() {
        enabledUser = User.builder().id(1L).email("user@example.com").password("ENC_PWD").enabled(true).build();
    }

    @Test
    void login_success() {
        AuthRequest req = new AuthRequest("user@example.com", "plain");
        when(userService.getByEmailOrThrow("user@example.com")).thenReturn(enabledUser);
        when(passwordEncoder.matches("plain", "ENC_PWD")).thenReturn(true);
        when(userRoleService.getRoleNamesByUserId(1L)).thenReturn(List.of("MEMBER"));
        when(jwtTokenProvider.generateToken("user@example.com")).thenReturn("jwt");

        AuthResponse resp = authService.login(req);
        assertEquals("jwt", resp.getToken());
        assertEquals(1L, resp.getUserId());
        assertEquals(List.of("MEMBER"), resp.getRoles());
    }

    @Test
    void login_wrongPassword_throws() {
        AuthRequest req = new AuthRequest("user@example.com", "wrong");
        when(userService.getByEmailOrThrow("user@example.com")).thenReturn(enabledUser);
        when(passwordEncoder.matches("wrong", "ENC_PWD")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_disabledUser_throws() {
        User disabled = User.builder().id(2L).email("user@example.com").password("ENC_PWD").enabled(false).build();
        AuthRequest req = new AuthRequest("user@example.com", "plain");
        when(userService.getByEmailOrThrow("user@example.com")).thenReturn(disabled);
        when(passwordEncoder.matches("plain", "ENC_PWD")).thenReturn(true);
        assertThrows(UserDisabledException.class, () -> authService.login(req));
    }

    @Test
    void login_noRoles_throws() {
        AuthRequest req = new AuthRequest("user@example.com", "plain");
        when(userService.getByEmailOrThrow("user@example.com")).thenReturn(enabledUser);
        when(passwordEncoder.matches("plain", "ENC_PWD")).thenReturn(true);
        when(userRoleService.getRoleNamesByUserId(1L)).thenReturn(List.of());
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void register_success() {
        AuthRequest req = new AuthRequest("new@example.com", "secret1");
        when(userService.emailExists("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("ENC2");
        User saved = User.builder().id(10L).email("new@example.com").password("ENC2").enabled(false).build();
        when(userService.save(any(User.class))).thenReturn(saved);
        Role member = Role.builder().id(5L).name("MEMBER").build();
        when(roleService.getOrCreateRole("MEMBER")).thenReturn(member);
        when(jwtTokenProvider.generateToken("new@example.com")).thenReturn("jwt2");

        AuthResponse resp = authService.register(req);
        assertEquals(10L, resp.getUserId());
        assertEquals("jwt2", resp.getToken());
        assertEquals(List.of("MEMBER"), resp.getRoles());
        verify(userRoleService).assignRolesToUsers(List.of(10L), List.of(5L));
    }

    @Test
    void register_emailExists_throws() {
        AuthRequest req = new AuthRequest("dup@example.com", "secret1");
        when(userService.emailExists("dup@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(req));
    }

    @Test
    void register_invalidFormat_throws() {
        AuthRequest req = new AuthRequest("bad", "123");
        assertThrows(InvalidCredentialsException.class, () -> authService.register(req));
    }
}
