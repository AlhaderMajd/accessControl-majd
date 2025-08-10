package com.example.accesscontrol.dto.role;

import com.example.accesscontrol.dto.permission.PermissionResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RoleDetailsResponse {
    private Long id;
    private String name;
    private List<PermissionResponse> permissions;
}
