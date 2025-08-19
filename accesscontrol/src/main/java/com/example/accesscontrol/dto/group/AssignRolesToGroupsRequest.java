package com.example.accesscontrol.dto.group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignRolesToGroupsRequest {
    private Long groupId;
    private List<Long> roleIds;
}
