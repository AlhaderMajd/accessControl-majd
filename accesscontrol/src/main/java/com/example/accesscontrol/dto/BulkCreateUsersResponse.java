package com.example.accesscontrol.dto;

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