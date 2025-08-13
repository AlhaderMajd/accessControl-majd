package com.example.accesscontrol.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    private List<Long> permissionIds;
}
