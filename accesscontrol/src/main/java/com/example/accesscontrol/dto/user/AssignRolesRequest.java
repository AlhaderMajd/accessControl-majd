package com.example.accesscontrol.dto.user;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolesRequest {
    private List<Long> userIds;
    private List<Long> roleIds;
}
