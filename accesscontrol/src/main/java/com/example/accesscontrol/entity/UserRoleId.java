package com.example.accesscontrol.entity;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class UserRoleId implements Serializable {
    private Long userId;
    private Long roleId;

    // Must have no-args constructor for JPA
    public UserRoleId() {}

    public UserRoleId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}
