package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "group_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_roles_group_role", columnNames = {"group_id", "role_id"}),
        indexes = {
                @Index(name = "idx_group_roles_group", columnList = "group_id"),
                @Index(name = "idx_group_roles_role", columnList = "role_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GroupRole {

    @EmbeddedId
    private Id id;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "group_id", nullable = false)
        private Long groupId;

        @Column(name = "role_id", nullable = false)
        private Long roleId;
    }
}
