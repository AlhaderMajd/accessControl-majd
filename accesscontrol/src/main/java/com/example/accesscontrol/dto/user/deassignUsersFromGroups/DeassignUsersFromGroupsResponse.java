package com.example.accesscontrol.dto.user.deassignUsersFromGroups;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeassignUsersFromGroupsResponse {
    private String message;
    private int removedCount;
}
