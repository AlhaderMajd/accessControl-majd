package com.example.accesscontrol.dto.user;

import lombok.Data;

@Data
public class AdminUpdateCredentialsRequest {
    private String email;
    private String password;
}
