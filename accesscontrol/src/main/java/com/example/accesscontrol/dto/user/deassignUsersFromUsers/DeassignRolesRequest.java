package com.example.accesscontrol.dto.user.deassignUsersFromUsers;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeassignRolesRequest {
    @NotEmpty
    private List<@NotNull Long> userIds;

    @NotEmpty
    private List<@NotNull Long> roleIds;
}
