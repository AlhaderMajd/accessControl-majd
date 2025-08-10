package com.example.accesscontrol.dto.user.updateCredentials;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUpdateCredentialsResponse {
    private String message;
    private Long id;
    private boolean emailUpdated;
    private boolean passwordUpdated;
}