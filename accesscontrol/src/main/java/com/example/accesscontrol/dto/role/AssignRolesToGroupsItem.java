package com.example.accesscontrol.dto.role;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignRolesToGroupsItem {
    private Long groupId;
    private List<Long> roleIds;
}
