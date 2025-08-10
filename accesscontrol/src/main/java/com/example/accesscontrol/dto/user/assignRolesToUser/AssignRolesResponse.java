package com.example.accesscontrol.dto.user.assignRolesToUser;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRolesResponse {
    private String message;
    private int assignedCount;
}
