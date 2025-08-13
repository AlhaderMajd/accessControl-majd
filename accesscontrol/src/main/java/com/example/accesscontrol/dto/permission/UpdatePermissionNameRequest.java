package com.example.accesscontrol.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePermissionNameRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
}
