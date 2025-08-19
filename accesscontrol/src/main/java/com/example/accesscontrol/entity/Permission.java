package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_name", columnNames = "name"),
        indexes = @Index(name = "idx_permissions_name", columnList = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Version
    private Long version;

    @Builder.Default
    @ManyToMany(mappedBy = "permissions")
    @BatchSize(size = 50)
    private Set<Role> roles = new LinkedHashSet<>();
}
