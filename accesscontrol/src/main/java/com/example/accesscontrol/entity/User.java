package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_enabled", columnList = "enabled")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Version
    private Long version;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"}))
    @BatchSize(size = 50)
    private Set<Role> roles = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "user_groups",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "group_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_groups_user_group", columnNames = {"user_id", "group_id"}))
    @BatchSize(size = 50)
    private Set<Group> groups = new LinkedHashSet<>();
}
