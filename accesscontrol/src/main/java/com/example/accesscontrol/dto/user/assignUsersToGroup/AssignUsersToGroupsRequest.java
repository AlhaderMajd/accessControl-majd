package com.example.accesscontrol.dto.user.assignUsersToGroup;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignUsersToGroupsRequest {
    @NotEmpty
    private List<@NotNull Long> userIds;

    @NotEmpty
    private List<@NotNull Long> groupIds;
}
