package com.example.accesscontrol.dto.user.deassignUsersFromUsers;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeassignRolesResponse {
    private String message;
    private int removedCount;
}
