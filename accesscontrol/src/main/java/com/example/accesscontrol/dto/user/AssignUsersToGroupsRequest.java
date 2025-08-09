package com.example.accesscontrol.dto.user;

import lombok.Data;
import java.util.List;

@Data
public class AssignUsersToGroupsRequest {
    private List<Long> userIds;
    private List<Long> groupIds;
}
