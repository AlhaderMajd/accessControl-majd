package com.example.accesscontrol.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotBlank(message = "Role name must not be blank")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;
}
