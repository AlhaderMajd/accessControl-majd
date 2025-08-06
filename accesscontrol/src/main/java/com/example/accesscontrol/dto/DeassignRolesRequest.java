package com.example.accesscontrol.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeassignRolesRequest {
    private List<Long> userIds;
    private List<Long> roleIds;
}
