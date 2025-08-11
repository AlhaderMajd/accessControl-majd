package com.example.accesscontrol.dto.permission;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreatePermissionsResponse {
    private String message;
    private int createdCount;
    private List<PermissionResponse> items;
}