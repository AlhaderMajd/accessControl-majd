package com.example.accesscontrol.dto.user;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateUsersResponse {
    private List<Long> createdUserIds;
    private List<String> assignedRoles;
}