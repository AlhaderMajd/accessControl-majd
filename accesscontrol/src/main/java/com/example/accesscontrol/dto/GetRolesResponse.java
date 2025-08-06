package com.example.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetRolesResponse {
    private List<RoleResponse> roles;
    private int page;
    private long total;
}
