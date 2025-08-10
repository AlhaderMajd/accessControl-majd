package com.example.accesscontrol.dto.user.deassignUsersFromUsers;

import lombok.Data;
import java.util.List;

@Data
public class DeassignRolesRequest {
    private List<Long> userIds;
    private List<Long> roleIds;
}
