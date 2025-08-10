package com.example.accesscontrol.dto.user.createUsers;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String email;
    private String password;
    private boolean enabled;
}