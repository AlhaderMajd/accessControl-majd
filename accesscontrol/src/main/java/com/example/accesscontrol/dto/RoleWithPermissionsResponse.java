package com.example.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleWithPermissionsResponse {
    private Long id;
    private String name;
    private List<PermissionResponse> permissions;
}
