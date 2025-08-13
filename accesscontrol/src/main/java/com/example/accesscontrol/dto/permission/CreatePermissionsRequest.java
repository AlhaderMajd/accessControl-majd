package com.example.accesscontrol.dto.permission;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatePermissionsRequest {
    @NotEmpty
    private List<@NotBlank @Size(max = 100) String> permissions;
}
