package com.example.accesscontrol.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupRoleId implements Serializable {
    private Long groupId;
    private Long roleId;
}
