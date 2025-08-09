package com.example.accesscontrol.dto.user;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailsResponse {
    private Long id;
    private String email;
    private boolean enabled;
    private List<String> roles;
    private List<String> groups;
}
