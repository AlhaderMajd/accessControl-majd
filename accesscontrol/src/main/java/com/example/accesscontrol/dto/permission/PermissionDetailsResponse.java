package com.example.accesscontrol.dto.permission;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionDetailsResponse {
    private Long id;
    private String name;
    private Long rolesCount;
}