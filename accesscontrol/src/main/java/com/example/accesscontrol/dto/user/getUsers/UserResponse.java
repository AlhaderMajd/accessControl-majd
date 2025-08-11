package com.example.accesscontrol.dto.user.getUsers;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private boolean enabled;
    private List<String> roles;
    private List<String> groups;
}
