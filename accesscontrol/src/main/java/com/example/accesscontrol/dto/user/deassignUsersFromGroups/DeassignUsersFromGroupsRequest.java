package com.example.accesscontrol.dto.user.deassignUsersFromGroups;

import lombok.Data;
import java.util.List;

@Data
public class DeassignUsersFromGroupsRequest {
    private List<Long> userIds;
    private List<Long> groupIds;
}
