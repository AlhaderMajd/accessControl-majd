package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserService userService;
    @Mock private UserRoleService userRoleService;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_success_buildsAuthoritiesAndFlags() {
        User u = User.builder().id(5L).email("john@example.com").password("enc").enabled(true).build();
        when(userService.getByEmailOrThrow("john@example.com")).thenReturn(u);
        when(userRoleService.getRoleNamesByUserId(5L)).thenReturn(List.of("ADMIN", "USER"));

        UserDetails details = customUserDetailsService.loadUserByUsername("john@example.com");

        assertEquals("john@example.com", details.getUsername());
        assertEquals("enc", details.getPassword());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
        // authorities should contain ROLE_ prefix
        List<String> auths = details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(auths.contains("ROLE_ADMIN"));
        assertTrue(auths.contains("ROLE_USER"));
    }

    @Test
    void loadUserByUsername_disabledUser_setsDisabledFlag() {
        User u = User.builder().id(2L).email("a@b.c").password("p").enabled(false).build();
        when(userService.getByEmailOrThrow("a@b.c")).thenReturn(u);
        when(userRoleService.getRoleNamesByUserId(2L)).thenReturn(List.of());

        UserDetails details = customUserDetailsService.loadUserByUsername("a@b.c");

        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsername_userNotFound_propagatesException() {
        when(userService.getByEmailOrThrow("x@y.z")).thenThrow(new UserNotFoundException("not found"));
        assertThrows(RuntimeException.class, () -> customUserDetailsService.loadUserByUsername("x@y.z"));
    }
}
