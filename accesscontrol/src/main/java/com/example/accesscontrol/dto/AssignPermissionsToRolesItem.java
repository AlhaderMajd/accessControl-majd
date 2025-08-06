package com.example.accesscontrol.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssignPermissionsToRolesItem {
    @NotNull
    private Long roleId;

    @NotNull
    private List<Long> permissionIds;
}
