package com.example.accesscontrol.dto;

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