package com.example.accesscontrol.dto.auth;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAuthResponse {
    private Long userId;
    private List<String> roles;
}
