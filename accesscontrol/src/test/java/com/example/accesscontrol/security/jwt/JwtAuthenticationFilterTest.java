package com.example.accesscontrol.security.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validToken_setsAuthenticationAndContinuesChain() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(email);
        UserDetails user = new User(email, "{noop}pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set in SecurityContext");
        assertEquals(user, auth.getPrincipal(), "Principal must be the loaded UserDetails");
        assertTrue(auth.isAuthenticated(), "Authentication should be marked as authenticated");
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getEmailFromToken(token);
        verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    void doFilter_missingToken_doesNotSetAuthenticationButContinues() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when no token provided");
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }

    @Test
    void doFilter_exceptionDuringProcessing_isCaught_noAuthSet_andContinues() throws ServletException, IOException {
        String token = "some.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when exception occurs");
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getEmailFromToken(token);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilter_nonBearerAuthorizationHeader_ignored_noValidateCall() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc.def.ghi"); // not starting with "Bearer "
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when header does not start with 'Bearer '");
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }

    @Test
    void doFilter_blankAuthorizationHeader_ignored_noInteractions() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "   "); // blank value
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when header is blank");
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }
    
    @Test
    void doFilter_tokenPresent_butValidateFalse_noAuth_noFurtherCalls() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when token validation fails");
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilter_validateTokenThrows_exceptionCaught_noAuth() throws ServletException, IOException {
        String token = "throwing.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("boom"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when validateToken throws");
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilter_bearerPrefixWithoutToken_ignored_noProviderCalls() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer "); // empty token after prefix
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must remain null when token is empty after Bearer prefix");
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }
}
