package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(name = "uk_role_permissions_role_perm", columnNames = {"role_id", "permission_id"}),
        indexes = {
                @Index(name = "idx_role_permissions_role", columnList = "role_id"),
                @Index(name = "idx_role_permissions_permission", columnList = "permission_id")
        })
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {

    @EmbeddedId
    private Id id;

    @Embeddable
    @Data
    @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "role_id", nullable = false)
        private Long roleId;

        @Column(name = "permission_id", nullable = false)
        private Long permissionId;
    }
}
