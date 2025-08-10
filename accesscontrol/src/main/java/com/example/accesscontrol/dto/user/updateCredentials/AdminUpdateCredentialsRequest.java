package com.example.accesscontrol.dto.user.updateCredentials;

import lombok.Data;

@Data
public class AdminUpdateCredentialsRequest {
    private String email;
    private String password;
}
