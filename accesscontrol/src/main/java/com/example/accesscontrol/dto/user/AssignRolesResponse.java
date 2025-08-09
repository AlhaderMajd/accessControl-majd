package com.example.accesscontrol.dto.user;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRolesResponse {
    private String message;
    private int assignedCount;
}
