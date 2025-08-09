package com.example.accesscontrol.dto.permission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePermissionNameResponse {
    private String message;
    private Long id;
    private String oldName;
    private String newName;
}
