package com.example.accesscontrol.dto.role;

import lombok.Data;
import java.util.List;

@Data
public class CreateRoleRequest {
    private String name;
    private List<Long> permissionIds; // optional
}
