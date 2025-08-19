//package com.example.accesscontrol.service;
//
//import com.example.accesscontrol.dto.auth.AuthRequest;
//import com.example.accesscontrol.dto.auth.LoginAuthResponse;
//import com.example.accesscontrol.entity.Role;
//import com.example.accesscontrol.entity.User;
//import com.example.accesscontrol.exception.EmailAlreadyUsedException;
//import com.example.accesscontrol.exception.InvalidCredentialsException;
//import com.example.accesscontrol.exception.UserDisabledException;
//import com.example.accesscontrol.security.jwt.JwtTokenProvider;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthServiceTest {
//
//    private static final String EXISTING_EMAIL = "user@example.com";
//    private static final String EXISTING_ENC_PWD = "ENC_PWD";
//    private static final long EXISTING_USER_ID = 1L;
//
//    private static final String NEW_EMAIL = "new@example.com";
//    private static final String NEW_PLAIN_PWD = "secret1";
//    private static final String NEW_ENC_PWD = "ENC2";
//    private static final long NEW_USER_ID = 10L;
//
//    private static final String MEMBER = "MEMBER";
//    private static final long MEMBER_ROLE_ID = 5L;
//
//    @Mock private UserService userService;
//    @Mock private UserRoleService userRoleService;
//    @Mock private RoleService roleService;
//    @Mock private JwtTokenProvider jwtTokenProvider;
//    @Mock private PasswordEncoder passwordEncoder;
//
//    @InjectMocks private AuthService authService;
//
//    private User enabledUser;
//
//    @BeforeEach
//    void setUp() {
//        enabledUser = User.builder()
//                .id(EXISTING_USER_ID)
//                .email(EXISTING_EMAIL)
//                .password(EXISTING_ENC_PWD)
//                .enabled(true)
//                .build();
//    }
//
//    @Test
//    @DisplayName("login success")
//    void login_success() {
//        AuthRequest req = new AuthRequest(EXISTING_EMAIL, "plain");
//        when(userService.getByEmailOrThrow(EXISTING_EMAIL)).thenReturn(enabledUser);
//        when(passwordEncoder.matches("plain", EXISTING_ENC_PWD)).thenReturn(true);
//        when(userRoleService.getRoleNamesByUserId(EXISTING_USER_ID)).thenReturn(List.of(MEMBER));
//        when(jwtTokenProvider.generateToken(EXISTING_EMAIL)).thenReturn("jwt");
//
//        LoginAuthResponse resp = authService.login(req);
//
//        assertEquals("jwt", resp.getToken());
//        assertEquals(EXISTING_USER_ID, resp.getUserId());
//        assertEquals(List.of(MEMBER), resp.getRoles());
//        verify(userService).getByEmailOrThrow(EXISTING_EMAIL);
//        verify(passwordEncoder).matches("plain", EXISTING_ENC_PWD);
//        verify(userRoleService).getRoleNamesByUserId(EXISTING_USER_ID);
//        verify(jwtTokenProvider).generateToken(EXISTING_EMAIL);
//    }
//
//    @Test
//    @DisplayName("login wrong password throws")
//    void login_wrongPassword_throws() {
//        AuthRequest req = new AuthRequest(EXISTING_EMAIL, "wrong");
//        when(userService.getByEmailOrThrow(EXISTING_EMAIL)).thenReturn(enabledUser);
//        when(passwordEncoder.matches("wrong", EXISTING_ENC_PWD)).thenReturn(false);
//
//        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
//        verify(jwtTokenProvider, never()).generateToken(anyString());
//    }
//
//    @Test
//    @DisplayName("login disabled user throws")
//    void login_disabledUser_throws() {
//        User disabled = User.builder()
//                .id(2L)
//                .email(EXISTING_EMAIL)
//                .password(EXISTING_ENC_PWD)
//                .enabled(false)
//                .build();
//        AuthRequest req = new AuthRequest(EXISTING_EMAIL, "plain");
//        when(userService.getByEmailOrThrow(EXISTING_EMAIL)).thenReturn(disabled);
//        when(passwordEncoder.matches("plain", EXISTING_ENC_PWD)).thenReturn(true);
//
//        assertThrows(UserDisabledException.class, () -> authService.login(req));
//        verify(jwtTokenProvider, never()).generateToken(anyString());
//    }
//
//    @Test
//    @DisplayName("login no roles throws")
//    void login_noRoles_throws() {
//        AuthRequest req = new AuthRequest(EXISTING_EMAIL, "plain");
//        when(userService.getByEmailOrThrow(EXISTING_EMAIL)).thenReturn(enabledUser);
//        when(passwordEncoder.matches("plain", EXISTING_ENC_PWD)).thenReturn(true);
//        when(userRoleService.getRoleNamesByUserId(EXISTING_USER_ID)).thenReturn(List.of());
//
//        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
//        verify(jwtTokenProvider, never()).generateToken(anyString());
//    }
//
//    @Test
//    @DisplayName("register success (token is null per service, role MEMBER assigned, user disabled)")
//    void register_success() {
//        AuthRequest req = new AuthRequest(NEW_EMAIL, NEW_PLAIN_PWD);
//
//        when(userService.emailExists(NEW_EMAIL)).thenReturn(false);
//        when(passwordEncoder.encode(NEW_PLAIN_PWD)).thenReturn(NEW_ENC_PWD);
//
//        User saved = User.builder()
//                .id(NEW_USER_ID)
//                .email(NEW_EMAIL)
//                .password(NEW_ENC_PWD)
//                .enabled(false)
//                .build();
//        when(userService.save(any(User.class))).thenReturn(saved);
//
//        Role member = Role.builder().id(MEMBER_ROLE_ID).name(MEMBER).build();
//        when(roleService.getOrCreateRole(MEMBER)).thenReturn(member);
//
//        LoginAuthResponse resp = authService.register(req);
//
//        assertEquals(NEW_USER_ID, resp.getUserId());
//        assertNull(resp.getToken());
//        assertEquals(List.of(MEMBER), resp.getRoles());
//        verify(userRoleService).assignRolesToUsers(List.of(NEW_USER_ID), List.of(MEMBER_ROLE_ID));
//
//        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
//        verify(userService).save(userCap.capture());
//        User toSave = userCap.getValue();
//        assertEquals(NEW_EMAIL, toSave.getEmail());
//        assertEquals(NEW_ENC_PWD, toSave.getPassword());
//        assertFalse(toSave.isEnabled());
//        verifyNoInteractions(jwtTokenProvider);
//    }
//
//    @Test
//    @DisplayName("register email exists throws")
//    void register_emailExists_throws() {
//        AuthRequest req = new AuthRequest("dup@example.com", NEW_PLAIN_PWD);
//        when(userService.emailExists("dup@example.com")).thenReturn(true);
//
//        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(req));
//        verify(userService, never()).save(any());
//        verify(userRoleService, never()).assignRolesToUsers(anyList(), anyList());
//        verifyNoInteractions(jwtTokenProvider);
//    }
//
//    @Test
//    @DisplayName("register invalid format throws")
//    void register_invalidFormat_throws() {
//        AuthRequest req = new AuthRequest("bad", "123");
//        assertThrows(InvalidCredentialsException.class, () -> authService.register(req));
//        verifyNoInteractions(userService, userRoleService, roleService, jwtTokenProvider, passwordEncoder);
//    }
//}
