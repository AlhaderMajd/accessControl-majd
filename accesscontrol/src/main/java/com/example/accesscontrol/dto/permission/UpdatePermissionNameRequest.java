package com.example.accesscontrol.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePermissionNameRequest {
    private String name;
}
