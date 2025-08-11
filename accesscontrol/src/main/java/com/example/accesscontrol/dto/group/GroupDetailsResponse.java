package com.example.accesscontrol.dto.group;

import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDetailsResponse {
    private Long id;
    private String name;
    private List<UserSummaryResponse> users;
    private List<RoleResponse> roles;
}
