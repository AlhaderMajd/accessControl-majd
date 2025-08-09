package com.example.accesscontrol.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignUsersToGroupsResponse {
    private String message;
    private int assignedCount;
}
