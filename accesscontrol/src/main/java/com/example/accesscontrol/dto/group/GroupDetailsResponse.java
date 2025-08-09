package com.example.accesscontrol.dto.group;

import com.example.accesscontrol.dto.role.RoleResponse;
import com.example.accesscontrol.dto.user.UserSummaryResponse;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupDetailsResponse {
    private Long id;
    private String name;
    private List<UserSummaryResponse> users;
    private List<RoleResponse> roles;
}
