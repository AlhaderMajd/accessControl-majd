package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(GroupRoleId.class)
public class GroupRole {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "role_id")
    private Long roleId;
}
