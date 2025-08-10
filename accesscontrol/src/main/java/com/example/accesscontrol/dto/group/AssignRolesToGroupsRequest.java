package com.example.accesscontrol.dto.group;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AssignRolesToGroupsRequest {
    private Long groupId;
    private List<Long> roleIds;
}
