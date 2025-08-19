package com.example.accesscontrol.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsToRolesRequest {
    private Long roleId;
    private List<Long> permissionIds;
}
