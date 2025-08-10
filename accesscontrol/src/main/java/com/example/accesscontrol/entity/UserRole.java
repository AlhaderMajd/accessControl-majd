package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "user_roles", uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"}),
        indexes = {
                @Index(name = "idx_user_roles_user", columnList = "user_id"),
                @Index(name = "idx_user_roles_role", columnList = "role_id")
        })
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserRole {

    @EmbeddedId
    private Id id;

    @Embeddable
    @Data
    @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "role_id", nullable = false)
        private Long roleId;
    }
}
