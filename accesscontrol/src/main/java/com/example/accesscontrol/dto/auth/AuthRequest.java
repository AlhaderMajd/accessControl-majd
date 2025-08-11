package com.example.accesscontrol.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @Email
    @NotBlank
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(min = 6, max = 255)
    private String password;
}
