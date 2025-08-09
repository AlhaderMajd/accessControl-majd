package com.example.accesscontrol.dto.role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignPermissionsToRolesResponse {
    private String message;
    private int assignedCount;
    private Integer rolesUpdated;
}
