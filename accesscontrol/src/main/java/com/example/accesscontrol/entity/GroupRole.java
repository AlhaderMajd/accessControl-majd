package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "group_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_roles_group_role",
                columnNames = {"group_id", "role_id"}
        ),
        indexes = {
                @Index(name = "idx_group_roles_group", columnList = "group_id"),
                @Index(name = "idx_group_roles_role", columnList = "role_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"group", "role"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_group_roles_group"))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_group_roles_role"))
    private Role role;

    @Version
    private long version;
}
