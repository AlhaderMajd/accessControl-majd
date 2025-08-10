package com.example.accesscontrol.dto.permission;

import lombok.Data;
import java.util.List;

@Data
public class CreatePermissionsRequest {
    private List<String> permissions;
}