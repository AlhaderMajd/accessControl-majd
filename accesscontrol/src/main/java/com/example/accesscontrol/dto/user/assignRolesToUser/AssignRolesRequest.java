package com.example.accesscontrol.dto.user.assignRolesToUser;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolesRequest {
    private List<Long> userIds;
    private List<Long> roleIds;
}
