package com.example.accesscontrol.dto.permission;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePermissionsRequest {
    @NotEmpty
    private List<@NotBlank @Size(max = 100) String> permissions;
}
