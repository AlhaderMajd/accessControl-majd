package com.example.accesscontrol.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsToRolesRequest {
    @NotNull
    private Long roleId;

    @NotEmpty
    private List<@NotNull Long> permissionIds;
}
