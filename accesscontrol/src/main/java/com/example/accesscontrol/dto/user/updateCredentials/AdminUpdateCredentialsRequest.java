package com.example.accesscontrol.dto.user.updateCredentials;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateCredentialsRequest {

    @Email
    @Size(max = 150)
    private String email;      // optional

    @Size(min = 6, max = 255)
    private String password;   // optional

    @AssertTrue(message = "At least one of email or password must be provided")
    public boolean isAnyProvided() {
        return (email != null && !email.isBlank()) || (password != null && !password.isBlank());
    }
}
