package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name"),
        indexes = @Index(name = "idx_roles_name", columnList = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Version
    private Long version;

    @Builder.Default
    @ManyToMany(mappedBy = "roles")
    @BatchSize(size = 50)
    private Set<User> users = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "permission_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_role_permissions_role_permission", columnNames = {"role_id", "permission_id"}))
    @BatchSize(size = 50)
    private Set<Permission> permissions = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "group_roles",
            joinColumns = @JoinColumn(name = "role_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "group_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_group_roles_group_role", columnNames = {"group_id", "role_id"}))
    @BatchSize(size = 50)
    private Set<Group> groups = new LinkedHashSet<>();
}
