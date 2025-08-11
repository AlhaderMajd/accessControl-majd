package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_returnsOkWithBody() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setToken("jwt-token");

        when(authService.login(any(AuthRequest.class))).thenReturn(resp);

        String body = "{\"email\":\"a@b.com\",\"password\":\"secret\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("jwt-token")));
    }

    @Test
    void register_returnsCreated() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setToken("new-token");
        Mockito.when(authService.register(any(AuthRequest.class))).thenReturn(resp);

        String body = "{\"email\":\"x@y.com\",\"password\":\"pass123\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("new-token")));
    }
}
