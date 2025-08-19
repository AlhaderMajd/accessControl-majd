//package com.example.accesscontrol.controller;
//
//import com.example.accesscontrol.dto.auth.AuthRequest;
//import com.example.accesscontrol.dto.auth.LoginAuthResponse;
//import com.example.accesscontrol.exception.EmailAlreadyUsedException;
//import com.example.accesscontrol.exception.GlobalExceptionHandler;
//import com.example.accesscontrol.exception.InvalidCredentialsException;
//import com.example.accesscontrol.exception.UserDisabledException;
//import com.example.accesscontrol.exception.UserNotFoundException;
//import com.example.accesscontrol.service.AuthService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(AuthController.class)
//@AutoConfigureMockMvc(addFilters = false)
//@Import(GlobalExceptionHandler.class)
//class AuthControllerTest {
//
//    @Autowired private MockMvc mockMvc;
//    @Autowired private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private AuthService authService;
//
//    private String json(Object o) throws Exception {
//        return objectMapper.writeValueAsString(o);
//    }
//
//    @Nested
//    @DisplayName("POST /api/auth/login")
//    class LoginTests {
//
//        @Test
//        @DisplayName("200 OK on valid login")
//        void login_ok() throws Exception {
//            var req = new AuthRequest("user@acme.test", "Secret#1");
//            var resp = LoginAuthResponse.builder()
//                    .token("token-123")
//                    .userId(42L)
//                    .roles(List.of("ADMIN", "MEMBER"))
//                    .build();
//
//            when(authService.login(any(AuthRequest.class))).thenReturn(resp);
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.token").value("token-123"))
//                    .andExpect(jsonPath("$.userId").value(42))
//                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
//
//            ArgumentCaptor<AuthRequest> cap = ArgumentCaptor.forClass(AuthRequest.class);
//            verify(authService).login(cap.capture());
//            assertThat(cap.getValue().getEmail()).isEqualTo("user@acme.test");
//            assertThat(cap.getValue().getPassword()).isEqualTo("Secret#1");
//        }
//
//        @Test
//        @DisplayName("400 Bad Request on validation failure")
//        void login_validation_400() throws Exception {
//            var bad = new AuthRequest("not-an-email", "123");
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(bad)))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(authService);
//        }
//
//        @Test
//        @DisplayName("400 Bad Request on missing body")
//        void login_missingBody_400() throws Exception {
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(authService);
//        }
//
//        @Test
//        @DisplayName("401 Unauthorized on invalid credentials")
//        void login_invalidCreds_401() throws Exception {
//            var req = new AuthRequest("user@acme.test", "Wrong#1");
//            when(authService.login(any(AuthRequest.class)))
//                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isUnauthorized())
//                    .andExpect(jsonPath("$.status").value(401))
//                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
//
//            verify(authService).login(any(AuthRequest.class));
//        }
//
//        @Test
//        @DisplayName("403 Forbidden when user is disabled")
//        void login_userDisabled_403() throws Exception {
//            var req = new AuthRequest("user@acme.test", "Secret#1");
//            when(authService.login(any(AuthRequest.class)))
//                    .thenThrow(new UserDisabledException());
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.status").value(403))
//                    .andExpect(jsonPath("$.error").value("User account is disabled"));
//
//            verify(authService).login(any(AuthRequest.class));
//        }
//
//        @Test
//        @DisplayName("404 Not Found when user email doesn't exist")
//        void login_notFound_404() throws Exception {
//            var req = new AuthRequest("missing@acme.test", "Secret#1");
//            // IMPORTANT: pass just the email; the exception class formats the message.
//            when(authService.login(any(AuthRequest.class)))
//                    .thenThrow(new UserNotFoundException("missing@acme.test"));
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.status").value(404))
//                    .andExpect(jsonPath("$.error").value("User not found with email: missing@acme.test"));
//
//            verify(authService).login(any(AuthRequest.class));
//        }
//
//        @Test
//        @DisplayName("500 Internal Server Error on unexpected exception")
//        void login_unexpected_500() throws Exception {
//            var req = new AuthRequest("user@acme.test", "Secret#1");
//            when(authService.login(any(AuthRequest.class)))
//                    .thenThrow(new RuntimeException("boom"));
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isInternalServerError())
//                    .andExpect(jsonPath("$.status").value(500))
//                    .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Something went wrong")));
//
//            verify(authService).login(any(AuthRequest.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("POST /api/auth/register")
//    class RegisterTests {
//
//        @Test
//        @DisplayName("201 Created on valid register")
//        void register_created_201() throws Exception {
//            var req = new AuthRequest("new@acme.test", "Secret#1");
//            var resp = LoginAuthResponse.builder()
//                    .token("t-abc")
//                    .userId(100L)
//                    .roles(List.of("MEMBER"))
//                    .build();
//
//            when(authService.register(any(AuthRequest.class))).thenReturn(resp);
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isCreated())
//                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                    .andExpect(jsonPath("$.token").value("t-abc"))
//                    .andExpect(jsonPath("$.userId").value(100))
//                    .andExpect(jsonPath("$.roles[0]").value("MEMBER"));
//
//            verify(authService).register(any(AuthRequest.class));
//        }
//
//        @Test
//        @DisplayName("400 Bad Request on validation failure")
//        void register_validation_400() throws Exception {
//            var bad = new AuthRequest("bad", "123");
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(bad)))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(authService);
//        }
//
//        @Test
//        @DisplayName("409 Conflict when email already used")
//        void register_emailInUse_409() throws Exception {
//            var req = new AuthRequest("dup@acme.test", "Secret#1");
//            when(authService.register(any(AuthRequest.class)))
//                    .thenThrow(new EmailAlreadyUsedException("Email already in use"));
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.status").value(409))
//                    .andExpect(jsonPath("$.error").value("Email already in use"));
//
//            verify(authService).register(any(AuthRequest.class));
//        }
//
//        @Test
//        @DisplayName("500 Internal Server Error on unexpected exception")
//        void register_unexpected_500() throws Exception {
//            var req = new AuthRequest("x@acme.test", "Secret#1");
//            when(authService.register(any(AuthRequest.class)))
//                    .thenThrow(new RuntimeException("oops"));
//
//            mockMvc.perform(post("/api/auth/register")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(json(req)))
//                    .andExpect(status().isInternalServerError())
//                    .andExpect(jsonPath("$.status").value(500))
//                    .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Something went wrong")));
//
//            verify(authService).register(any(AuthRequest.class));
//        }
//    }
//}
