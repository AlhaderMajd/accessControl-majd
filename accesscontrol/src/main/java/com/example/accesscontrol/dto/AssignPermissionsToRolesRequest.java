package com.example.accesscontrol.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsToRolesRequest {
    @NotEmpty(message = "Mappings list cannot be empty")
    private List<RolePermissionMapping> mappings;

    @Data
    public static class RolePermissionMapping {
        private Long roleId;
        private List<Long> permissionIds;
    }
}
