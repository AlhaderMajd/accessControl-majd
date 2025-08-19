package com.example.accesscontrol.dto.user.updateCredentials;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateCredentialsRequest {
    private String email;
    private String password;
}
