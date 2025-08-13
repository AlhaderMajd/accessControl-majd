package com.example.accesscontrol.dto.user.deassignUsersFromGroups;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeassignUsersFromGroupsRequest {
    @NotEmpty
    private List<@NotNull Long> userIds;

    @NotEmpty
    private List<@NotNull Long> groupIds;
}
