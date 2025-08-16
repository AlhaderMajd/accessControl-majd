package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;
    private final UserRoleService userRoleService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        final User u;
        try {
            u = userService.getByEmailOrThrow(email);
        } catch (UserNotFoundException ex) {
            throw new UsernameNotFoundException(ex.getMessage(), ex);
        }

        List<String> roleNames = userRoleService.getRoleNamesByUserId(u.getId());
        List<GrantedAuthority> authorities = roleNames.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getEmail())
                .password(u.getPassword())
                .disabled(!u.isEnabled())
                .authorities(authorities)
                .build();
    }
}
